@file:SuppressLint("ComposableNaming")

package coil.compose

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter.ExecuteCallback
import coil.compose.ImagePainter.State
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.OriginalSize
import coil.size.Precision
import coil.size.Scale
import coil.transition.CrossfadeTransition
import com.google.accompanist.drawablepainter.DrawablePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Return an [ImagePainter] that will execute an [ImageRequest] using [imageLoader].
 *
 * @param data The [ImageRequest.data] to load.
 * @param imageLoader The [ImageLoader] that will be used to execute the request.
 * @param onExecute Called immediately before the [ImagePainter] launches an image request.
 *  Return 'true' to proceed with the request. Return 'false' to skip executing the request.
 * @param builder An optional lambda to configure the request.
 */
@Composable
inline fun rememberImagePainter(
    data: Any?,
    imageLoader: ImageLoader,
    onExecute: ExecuteCallback = ExecuteCallback.Default,
    builder: ImageRequest.Builder.() -> Unit = {},
): ImagePainter {
    val request = ImageRequest.Builder(LocalContext.current)
        .data(data)
        .apply(builder)
        .build()
    return rememberImagePainter(request, imageLoader, onExecute)
}

/**
 * Return an [ImagePainter] that will execute the [request] using [imageLoader].
 *
 * @param request The [ImageRequest] to execute.
 * @param imageLoader The [ImageLoader] that will be used to execute [request].
 * @param onExecute Called immediately before the [ImagePainter] launches an image request.
 *  Return 'true' to proceed with the request. Return 'false' to skip executing the request.
 */
@Composable
fun rememberImagePainter(
    request: ImageRequest,
    imageLoader: ImageLoader,
    onExecute: ExecuteCallback = ExecuteCallback.Default,
): ImagePainter {
    requireSupportedData(request.data)
    require(request.target == null) { "request.target must be null." }

    val scope = rememberCoroutineScope { Dispatchers.Main.immediate }
    val imagePainter = remember(scope) { ImagePainter(scope, request, imageLoader) }
    imagePainter.request = request
    imagePainter.imageLoader = imageLoader
    imagePainter.onExecute = onExecute
    imagePainter.isPreview = LocalInspectionMode.current
    updatePainter(imagePainter, request, imageLoader)
    return imagePainter
}

/**
 * A [Painter] that asynchronously executes [ImageRequest]s and draws the result.
 * Instances can only be created with [rememberImagePainter].
 */
@Stable
class ImagePainter internal constructor(
    private val parentScope: CoroutineScope,
    request: ImageRequest,
    imageLoader: ImageLoader
) : Painter(), RememberObserver {

    private var rememberScope: CoroutineScope? = null
    private var requestJob: Job? = null
    private var drawSize: Size by mutableStateOf(Size.Zero)

    private var alpha: Float by mutableStateOf(1f)
    private var colorFilter: ColorFilter? by mutableStateOf(null)

    internal var painter: Painter? by mutableStateOf(null)
    internal var onExecute = ExecuteCallback.Default
    internal var isPreview = false

    /** The current [ImagePainter.State]. */
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
        drawSize = size

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

        // Create a new scope to observe state and execute requests while we're remembered.
        rememberScope?.cancel()
        val context = parentScope.coroutineContext
        val scope = CoroutineScope(context + SupervisorJob(context[Job]))
        rememberScope = scope

        // Observe the current request + request size and launch new requests as necessary.
        scope.launch {
            var snapshot: Snapshot? = null
            combine(
                snapshotFlow { request },
                snapshotFlow { drawSize },
                transform = ::Pair
            ).collect { (request, size) ->
                val previous = snapshot
                val current = Snapshot(state, request, size)
                snapshot = current

                // Short circuit if the size hasn't been set explicitly and the draw size is positive.
                if (request.defined.sizeResolver == null &&
                        size.isSpecified && (size.width <= 0.5f || size.height <= 0.5f)) {
                    state = State.Empty
                    return@collect
                }

                // Execute the image request.
                execute(previous, current)
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

    private fun CoroutineScope.execute(previous: Snapshot?, current: Snapshot) {
        if (!onExecute(previous, current)) return

        // Execute the image request.
        requestJob?.cancel()
        requestJob = launch {
            state = imageLoader.execute(updateRequest(current.request, current.size)).toState()
        }
    }

    /** Update the [request] to work with [ImagePainter]. */
    private fun updateRequest(request: ImageRequest, size: Size): ImageRequest {
        return request.newBuilder()
            .target(
                onStart = { placeholder ->
                    state = State.Loading(painter = placeholder?.toPainter())
                }
            )
            .apply {
                // Set the size unless it has been set explicitly.
                if (request.defined.sizeResolver == null) {
                    if (size.isSpecified) {
                        size(size.width.roundToInt(), size.height.roundToInt())
                    } else {
                        size(OriginalSize)
                    }
                }

                // Set the scale to fill unless it has been set explicitly.
                // We do this since it's not possible to auto-detect the scale type like with `ImageView`s.
                if (request.defined.scale == null) {
                    scale(Scale.FILL)
                }

                // Set inexact precision unless exact precision has been set explicitly.
                if (request.defined.precision != Precision.EXACT) {
                    precision(Precision.INEXACT)
                }
            }
            .build()
    }

    /**
     * Invoked immediately before the [ImagePainter] executes a new image request.
     * Return 'true' to proceed with the request. Return 'false' to skip executing the request.
     */
    @ExperimentalCoilApi
    fun interface ExecuteCallback {

        operator fun invoke(previous: Snapshot?, current: Snapshot): Boolean

        companion object {
            /**
             * Proceeds with the request if the painter is empty or the request has changed.
             * This **does not** proceed with the request if only the draw size has changed.
             */
            @JvmField val Default = ExecuteCallback { previous, current ->
                current.state == State.Empty || previous?.request != current.request
            }
        }
    }

    /**
     * A snapshot of the [ImagePainter]'s properties.
     */
    @ExperimentalCoilApi
    data class Snapshot(
        val state: State,
        val request: ImageRequest,
        val size: Size,
    )

    /**
     * The current state of the [ImagePainter].
     */
    @ExperimentalCoilApi
    sealed class State {

        /** The current painter being drawn by [ImagePainter]. */
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
            val metadata: ImageResult.Metadata,
        ) : State()

        /** The request failed due to [throwable]. */
        data class Error(
            override val painter: Painter?,
            val throwable: Throwable,
        ) : State()
    }
}

/**
 * Allows us to observe the current [ImagePainter.painter]. This function allows us to
 * minimize the amount of recomposition needed such that this function only needs to be restarted
 * when the [ImagePainter.state] changes.
 */
@Composable
private fun updatePainter(
    imagePainter: ImagePainter,
    request: ImageRequest,
    imageLoader: ImageLoader
) {
    // If we're in inspection mode (preview) and we have a placeholder, just draw
    // that without executing an image request.
    if (imagePainter.isPreview) {
        imagePainter.painter = request.placeholder?.toPainter()
        return
    }

    // This may look like a useless remember, but this allows any Painter instances
    // to receive remember events (if it implements RememberObserver). Do not remove.
    val state = imagePainter.state
    val painter = remember(state) { state.painter }

    // Short circuit if the crossfade transition isn't set.
    val transition = request.defined.transition ?: imageLoader.defaults.transition
    if (transition !is CrossfadeTransition) {
        imagePainter.painter = painter
        return
    }

    // Keep track of the most recent loading painter to crossfade from it.
    val loading = remember(request) { ValueHolder<Painter?>(null) }
    if (state is State.Loading) loading.value = state.painter

    // Short circuit if the request isn't successful or if it's returned by the memory cache.
    if (state !is State.Success || state.metadata.dataSource == DataSource.MEMORY_CACHE) {
        imagePainter.painter = painter
        return
    }

    // Set the crossfade painter.
    imagePainter.painter = rememberCrossfadePainter(
        key = state,
        start = loading.value,
        end = painter,
        // Fall back to Scale.FIT to match the default image content scale.
        scale = request.defined.scale ?: Scale.FIT,
        durationMillis = transition.durationMillis,
        fadeStart = !state.metadata.isPlaceholderMemoryCacheKeyPresent
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
        "Unsupported type: $name. If you wish to display this $name, use androidx.compose.foundation.Image."
    )
}

private fun ImageResult.toState() = when (this) {
    is SuccessResult -> State.Success(
        painter = drawable.toPainter(),
        metadata = metadata
    )
    is ErrorResult -> State.Error(
        painter = drawable?.toPainter(),
        throwable = throwable
    )
}

/** Convert this [Drawable] into a [Painter] using Compose primitives if possible. */
private fun Drawable.toPainter(): Painter {
    return when (this) {
        is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
        is ColorDrawable -> ColorPainter(Color(color))
        else -> DrawablePainter(mutate())
    }
}

/** A simple mutable value holder that avoids recomposition. */
private class ValueHolder<T>(@JvmField var value: T)
