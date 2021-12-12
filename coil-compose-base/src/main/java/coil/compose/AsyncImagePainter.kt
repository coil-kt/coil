@file:Suppress("ComposableNaming", "unused")

package coil.compose

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.SizeResolver
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.transition.TransitionTarget
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
import coil.size.Size as CoilSize

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

    private var painter: Painter? by mutableStateOf(null)
    private var alpha: Float by mutableStateOf(1f)
    private var colorFilter: ColorFilter? by mutableStateOf(null)

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
        // If we're in inspection mode (preview) skip executing the image request
        // and set the state to loading.
        if (isPreview) {
            val request = request.newBuilder().defaults(imageLoader.defaults).build()
            state = State.Loading(request.placeholder?.toPainter())
            return
        }

        // Short circuit if we're already remembered.
        if (rememberScope != null) return

        // Create a new scope to observe state and execute requests while we're remembered.
        val scope = parentScope + SupervisorJob(parentScope.coroutineContext.job)
        rememberScope = scope

        // Observe the current request + request size and launch new requests as necessary.
        scope.launch {
            snapshotFlow { request }.collect { request ->
                requestJob?.cancel()
                requestJob = launch {
                    updateState(imageLoader.execute(updateRequest(request)).toState())
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
                    updateState(State.Loading(placeholder?.toPainter()))
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

    private fun updateState(currentState: State) {
        val previousState = state
        state = currentState
        painter = getPainter(previousState, currentState)
    }

    /** Wrap the current state's painter in a [CrossfadePainter] if necessary and return it. */
    private fun getPainter(previous: State, current: State): Painter? {
        // We can only invoke the transition factory if the state is success or error.
        val result = when (current) {
            is State.Success -> current.result
            is State.Error -> current.result
            else -> return current.painter
        }

        // Invoke the transition factory and wrap the painter in a `CrossfadePainter` if it returns
        // a `CrossfadeTransformation`.
        val factory = request.defined.transitionFactory ?: imageLoader.defaults.transitionFactory
        val transition = factory.create(newTransitionTarget(request.context), result)
        return if (transition is CrossfadeTransition) {
            CrossfadePainter(
                start = (previous as? State.Loading)?.painter,
                end = painter,
                scale = request.scale,
                durationMillis = transition.durationMillis,
                fadeStart = result is SuccessResult && !result.isPlaceholderCached,
                preferExactIntrinsicSize = transition.preferExactIntrinsicSize
            )
        } else {
            current.painter
        }
    }

    private fun ImageResult.toState() = when (this) {
        is SuccessResult -> State.Success(drawable.toPainter(), this)
        is ErrorResult -> State.Error(drawable?.toPainter(), this)
    }

    /** Convert this [Drawable] into a [Painter] using Compose primitives if possible. */
    private fun Drawable.toPainter() = when (this) {
        is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap(), filterQuality = filterQuality)
        is ColorDrawable -> ColorPainter(Color(color))
        else -> DrawablePainter(mutate())
    }

    /** Suspends until the draw size for this [AsyncImagePainter] is unspecified or positive. */
    private inner class DrawSizeResolver : SizeResolver {

        override suspend fun size() = drawSize.mapNotNull { it.toSizeOrNull() }.first()
    }

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

private fun Size.toSizeOrNull() = when {
    isUnspecified -> CoilSize.ORIGINAL
    isPositive -> CoilSize(width.roundToInt(), height.roundToInt())
    else -> null
}

private val Size.isPositive get() = width >= 0.5 && height >= 0.5

/** Create a fake [TransitionTarget] so we can call [Transition.Factory.create] */
private fun newTransitionTarget(context: Context) = object : TransitionTarget {
    override val view: View get() = View(context)
    override val drawable: Drawable? get() = null
}

/** Create an [ImageRequest] from the [model]. */
@Composable
@ReadOnlyComposable
internal fun requestOf(model: Any?): ImageRequest {
    if (model is ImageRequest) {
        return model
    } else {
        return ImageRequest.Builder(LocalContext.current).data(model).build()
    }
}
