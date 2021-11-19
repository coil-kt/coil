@file:Suppress("ComposableNaming", "DEPRECATION_ERROR", "unused", "UNUSED_PARAMETER")

package coil.compose

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import coil.ImageLoader
import coil.compose.AsyncImagePainter.ExecuteCallback
import coil.compose.AsyncImagePainter.State
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Precision
import coil.size.SizeResolver
import coil.transition.CrossfadeTransition
import com.google.accompanist.drawablepainter.DrawablePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlin.math.roundToInt

/**
 * Return an [AsyncImagePainter] that executes an [ImageRequest] asynchronously and
 * renders the result.
 *
 * This is a lower-level API than [AsyncImage] and may not work as expected in all situations.
 * Notably, it will not finish loading if [AsyncImagePainter.onDraw] is not called, which can occur
 * for composables that don't have a fixed size (e.g. [LazyColumn]). It's recommended to use
 * [AsyncImage] unless you need a reference to a [Painter].
 *
 * @param model Either an [ImageRequest] or the [ImageRequest.data] value.
 * @param imageLoader The [ImageLoader] that will be used to execute the request.
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn
 *  into the destination.
 */
@Composable
fun rememberAsyncImagePainter(
    model: Any?,
    imageLoader: ImageLoader,
    filterQuality: FilterQuality = DefaultFilterQuality,
): AsyncImagePainter {
    val request = requestOf(model)
    requireSupportedData(request.data)
    require(request.target == null) { "request.target must be null." }

    val scope = rememberCoroutineScope { Dispatchers.Main.immediate }
    val painter = remember(scope) { AsyncImagePainter(scope, request, imageLoader) }
    painter.request = request
    painter.imageLoader = imageLoader
    painter.filterQuality = filterQuality
    painter.isPreview = LocalInspectionMode.current
    painter.onRemembered() // Invoke this manually so `painter.state` is up to date immediately.
    updatePainter(painter, request, imageLoader)
    return painter
}

/**
 * A [Painter] that that executes an [ImageRequest] asynchronously and renders the result.
 */
@Stable
class AsyncImagePainter internal constructor(
    private val parentScope: CoroutineScope,
    request: ImageRequest,
    imageLoader: ImageLoader
) : Painter(), RememberObserver {

    private var rememberScope: CoroutineScope? = null
    private var requestJob: Job? = null
    private var drawSize = MutableStateFlow(Size.Zero)

    private var alpha: Float by mutableStateOf(1f)
    private var colorFilter: ColorFilter? by mutableStateOf(null)

    internal var painter: Painter? by mutableStateOf(null)
    internal var filterQuality = DefaultFilterQuality
    internal var isPreview = false

    /** The current [AsyncImagePainter.State]. */
    var state: State by mutableStateOf(State.Empty)
        private set

    /** The current [ImageRequest]. */
    var request: ImageRequest by mutableStateOf(request)
        internal set

    /** The current [ImageLoader]. */
    var imageLoader: ImageLoader by mutableStateOf(imageLoader)
        internal set

    override val intrinsicSize: Size
        get() = painter?.intrinsicSize ?: Size.Unspecified

    override fun DrawScope.onDraw() {
        // Update the draw scope's current size.
        drawSize.value = size

        // Draw the current painter.
        painter?.apply { draw(size, alpha, colorFilter) }
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun onRemembered() {
        if (isPreview) return
        if (rememberScope != null) return

        // Create a new scope to observe state and execute requests while we're remembered.
        val scope = parentScope + SupervisorJob(parentScope.coroutineContext.job)
        rememberScope = scope

        // Observe the current request + request size and launch new requests as necessary.
        scope.launch {
            snapshotFlow { request }.collect { request ->
                requestJob?.cancel()
                requestJob = launch {
                    state = imageLoader.execute(updateRequest(request)).toState()
                }
            }
        }
    }

    override fun onForgotten() {
        rememberScope?.cancel()
        rememberScope = null
        requestJob?.cancel()
        requestJob = null
    }

    override fun onAbandoned() = onForgotten()

    /** Update the [request] to work with [AsyncImagePainter]. */
    private fun updateRequest(request: ImageRequest): ImageRequest {
        return request.newBuilder()
            .target(
                onStart = { placeholder ->
                    state = State.Loading(placeholder?.toPainter())
                }
            )
            .apply {
                if (request.defined.sizeResolver == null) {
                    size(DrawSizeResolver())
                }
                if (request.defined.precision != Precision.EXACT) {
                    precision(Precision.INEXACT)
                }
            }
            .build()
    }

    private fun ImageResult.toState() = when (this) {
        is SuccessResult -> State.Success(drawable.toPainter(), this)
        is ErrorResult -> State.Error(drawable?.toPainter(), this)
    }

    /** Convert this [Drawable] into a [Painter] using Compose primitives if possible. */
    internal fun Drawable.toPainter() = when (this) {
        is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap(), filterQuality = filterQuality)
        is ColorDrawable -> ColorPainter(Color(color))
        else -> DrawablePainter(mutate())
    }

    /** Suspends until the draw size for this [AsyncImagePainter] is unspecified or positive. */
    private inner class DrawSizeResolver : SizeResolver {

        override suspend fun size() = drawSize
            .mapNotNull { size ->
                when {
                    size.isUnspecified -> OriginalSize
                    size.isPositive -> PixelSize(size.width.roundToInt(), size.height.roundToInt())
                    else -> null
                }
            }
            .first()
    }

    @Deprecated(
        message = "ExecuteCallback no longer has any effect. To skip waiting for size resolution " +
            "use ImageRequest.Builder.size(OriginalSize).",
        level = DeprecationLevel.ERROR
    )
    fun interface ExecuteCallback {

        operator fun invoke(previous: Snapshot?, current: Snapshot): Boolean

        companion object {
            @JvmField val Default = ExecuteCallback { previous, current ->
                previous?.request != current.request
            }
        }
    }

    @Deprecated(
        message = "ExecuteCallback no longer has any effect. To skip waiting for size resolution " +
            "use ImageRequest.Builder.size(OriginalSize).",
        level = DeprecationLevel.ERROR
    )
    data class Snapshot(
        val state: State,
        val request: ImageRequest,
        val size: Size,
    )

    /**
     * The current state of the [AsyncImagePainter].
     */
    sealed class State {

        /** The current painter being drawn by [AsyncImagePainter]. */
        abstract val painter: Painter?

        /** The request has not been started. */
        object Empty : State() {
            override val painter: Painter? get() = null
        }

        /** The request is in-progress. */
        data class Loading(
            override val painter: Painter?,
        ) : State()

        /** The request was successful. */
        data class Success(
            override val painter: Painter,
            val result: SuccessResult,
        ) : State()

        /** The request failed due to [ErrorResult.throwable]. */
        data class Error(
            override val painter: Painter?,
            val result: ErrorResult,
        ) : State()
    }
}

/**
 * Allows us to observe the current [AsyncImagePainter.painter]. This function allows us to
 * minimize the amount of recomposition needed such that this function only needs to be restarted
 * when the [AsyncImagePainter.state] changes.
 */
@Composable
private fun updatePainter(
    imagePainter: AsyncImagePainter,
    request: ImageRequest,
    imageLoader: ImageLoader
) {
    // If we're in inspection mode (preview) and we have a placeholder, just draw
    // that without executing an image request.
    if (imagePainter.isPreview) {
        val newRequest = request.newBuilder().defaults(imageLoader.defaults).build()
        imagePainter.painter = with(imagePainter) { newRequest.placeholder?.toPainter() }
        return
    }

    // This may look like a useless remember, but this allows any painter instances
    // to receive remember events (if it implements RememberObserver). Do not remove.
    val state = imagePainter.state
    val painter = remember(state) { state.painter }

    // Short circuit if the crossfade transition isn't set.
    // Check `imageLoader.defaults.transitionFactory` specifically as the default isn't set
    // until the request is executed.
    val transition = request.defined.transitionFactory ?: imageLoader.defaults.transitionFactory
    if (transition !is CrossfadeTransition.Factory) {
        imagePainter.painter = painter
        return
    }

    // Keep track of the most recent loading painter to crossfade from it.
    val loading = remember(request) { ValueHolder<Painter?>(null) }
    if (state is State.Loading) loading.value = state.painter

    // Short circuit if the request isn't successful or if it's returned by the memory cache.
    if (state !is State.Success || state.result.dataSource == DataSource.MEMORY_CACHE) {
        imagePainter.painter = painter
        return
    }

    // Set the crossfade painter.
    imagePainter.painter = rememberCrossfadePainter(
        key = state,
        start = loading.value,
        end = painter,
        scale = request.scale,
        durationMillis = transition.durationMillis,
        fadeStart = !state.result.isPlaceholderCached,
        preferExactIntrinsicSize = transition.preferExactIntrinsicSize
    )
}

private fun requireSupportedData(data: Any?) = when (data) {
    is ImageBitmap -> unsupportedData("ImageBitmap")
    is ImageVector -> unsupportedData("ImageVector")
    is Painter -> unsupportedData("Painter")
    else -> data
}

private fun unsupportedData(name: String): Nothing {
    throw IllegalArgumentException(
        "Unsupported type: $name. If you wish to display this $name, " +
            "use androidx.compose.foundation.Image."
    )
}

private val Size.isPositive get() = width >= 0.5 && height >= 0.5

/** A simple mutable value holder that avoids recomposition. */
private class ValueHolder<T>(@JvmField var value: T)

/** Create an [ImageRequest] using the [model]. */
@Composable
internal fun requestOf(model: Any?): ImageRequest {
    return if (model is ImageRequest) {
        return model
    } else {
        ImageRequest.Builder(LocalContext.current).data(model).build()
    }
}

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "AsyncImagePainter",
        imports = ["coil.compose.AsyncImagePainter"]
    )
)
typealias ImagePainter = AsyncImagePainter

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(" +
            "ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build(), " +
            "imageLoader)",
        imports = [
            "androidx.compose.ui.platform.LocalContext",
            "coil.compose.rememberAsyncImagePainter",
            "coil.request.ImageRequest",
        ]
    )
)
@Composable
inline fun rememberImagePainter(
    data: Any?,
    imageLoader: ImageLoader,
    onExecute: ExecuteCallback = ExecuteCallback.Default,
    builder: ImageRequest.Builder.() -> Unit = {},
) = rememberAsyncImagePainter(
    model = ImageRequest.Builder(LocalContext.current).data(data).apply(builder).build(),
    imageLoader = imageLoader
)

@Deprecated(
    message = "ImagePainter has been renamed to AsyncImagePainter.",
    replaceWith = ReplaceWith(
        expression = "rememberAsyncImagePainter(request, imageLoader)",
        imports = ["coil.compose.rememberAsyncImagePainter"]
    )
)
@Composable
fun rememberImagePainter(
    request: ImageRequest,
    imageLoader: ImageLoader,
    onExecute: ExecuteCallback = ExecuteCallback.Default
) = rememberAsyncImagePainter(
    model = request,
    imageLoader = imageLoader
)
