package coil.compose

import android.annotation.SuppressLint
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
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter.ExecuteCallback
import coil.compose.ImagePainter.State
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Precision
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
 * Use [ImageRequest.Builder.fadeIn] to configure the fade in animation duration.
 *
 * @param data The [ImageRequest.data] to execute.
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
 * Use [ImageRequest.Builder.fadeIn] to configure the fade in animation duration.
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
    val painter = remember(scope) { ImagePainter(scope, request, imageLoader) }
    painter.request = request
    painter.imageLoader = imageLoader
    painter.onExecute = onExecute
    painter.isPreview = LocalInspectionMode.current
    painter.rootViewSize = LocalView.current.run { IntSize(width, height) }

    updatePainter(painter, request)
    updateFadeInTransition(painter, request.parameters.fadeInMillis() ?: 0)

    return painter
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

    private var lazyPaint: Paint? = null
    private val paint: Paint get() = lazyPaint ?: Paint().also { lazyPaint = it }

    private var scope: CoroutineScope? = null
    private var requestJob: Job? = null
    private var requestSize: IntSize by mutableStateOf(IntSize.Zero)

    private var alpha: Float by mutableStateOf(1f)
    private var colorFilter: ColorFilter? by mutableStateOf(null)

    internal var painter: Painter by mutableStateOf(EmptyPainter)
    internal var transitionColorFilter: ColorFilter? by mutableStateOf(null)
    internal var onExecute = ExecuteCallback.Default
    internal var isPreview = false
    internal var rootViewSize = IntSize.Zero

    /** The current [ImagePainter.State]. */
    var state: State by mutableStateOf(State.Empty)
        private set

    /** The current [ImageRequest]. */
    var request: ImageRequest by mutableStateOf(request)
        internal set

    /** The current [ImageLoader]. */
    var imageLoader: ImageLoader by mutableStateOf(imageLoader)
        internal set

    override val intrinsicSize get() = painter.intrinsicSize

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        // Update the request size based on the canvas size.
        updateRequestSize(canvasSize = size)

        if (colorFilter != null && transitionColorFilter != null) {
            // If we have a transition color filter and a specified color filter we need to
            // draw the content in a layer for both to apply.
            // https://github.com/google/accompanist/issues/262
            drawIntoCanvas { canvas ->
                canvas.withSaveLayer(
                    bounds = size.toRect(),
                    paint = paint.apply { colorFilter = transitionColorFilter }
                ) { drawPainter() }
            }
        } else {
            // Else we just draw the content as usual.
            drawPainter()
        }
    }

    private fun DrawScope.drawPainter() = with(painter) {
        draw(size, alpha, colorFilter ?: transitionColorFilter)
    }

    override fun onRemembered() {
        if (isPreview) return
        scope?.cancel()
        val context = parentScope.coroutineContext
        val scope = CoroutineScope(context + SupervisorJob(context[Job])).also { scope = it }

        // Observe the current request + request size and launch new requests as necessary.
        scope.launch {
            var snapshot: Snapshot? = null
            combine(
                snapshotFlow { request },
                snapshotFlow { requestSize },
                transform = ::Pair
            ).collect { (request, size) ->
                val previous = snapshot
                val current = Snapshot(state, request, size)
                snapshot = current

                // Skip the size check if the size has been set explicitly.
                if (request.defined.sizeResolver != null) {
                    execute(previous, current)
                    return@collect
                }

                // Short circuit if the requested size is 0.
                if (size == IntSize.Zero) {
                    state = State.Empty
                    return@collect
                }

                // Execute the image request.
                execute(previous, current)
            }
        }
    }

    override fun onForgotten() {
        scope?.cancel()
        scope = null
        requestJob?.cancel()
        requestJob = null
    }

    override fun onAbandoned() = onForgotten()

    private fun updateRequestSize(canvasSize: Size) {
        requestSize = IntSize(
            width = when {
                // If we have a canvas width, use it...
                canvasSize.width >= 0.5f -> canvasSize.width.roundToInt()
                // Otherwise we fall-back to the root view size as an upper bound.
                else -> rootViewSize.width.coerceAtLeast(0)
            },
            height = when {
                // If we have a canvas height, use it...
                canvasSize.height >= 0.5f -> canvasSize.height.roundToInt()
                // Otherwise we fall-back to the root view size as an upper bound.
                else -> rootViewSize.height.coerceAtLeast(0)
            }
        )
    }

    private fun execute(previous: Snapshot?, current: Snapshot) {
        val scope = scope ?: return // Shouldn't happen.
        if (!onExecute(previous, current)) return

        // Execute the image request.
        requestJob?.cancel()
        requestJob = scope.launch {
            val request = current.request
            val newRequest = request.newBuilder()
                .target(
                    onStart = { placeholder ->
                        state = State.Loading(painter = placeholder?.toPainter())
                    }
                )
                .apply {
                    // Set the size if it hasn't already been set and it's valid.
                    val size = current.size
                    if (request.defined.sizeResolver == null && size.width > 0 && size.height > 0) {
                        size(size.width, size.height)
                    }

                    // Use inexact precision unless exact precision has been set explicitly.
                    if (request.defined.precision != Precision.EXACT) {
                        precision(Precision.INEXACT)
                    }
                }
                .build()
            state = imageLoader.execute(newRequest).toState()
        }
    }

    /**
     * Invoked immediately before the [ImagePainter] executes a new image request.
     * Return 'true' to proceed with the request. Return 'false' to skip executing the request.
     */
    @ExperimentalCoilApi
    fun interface ExecuteCallback {

        operator fun invoke(previous: Snapshot?, current: Snapshot): Boolean

        companion object {
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
        val size: IntSize,
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
@SuppressLint("ComposableNaming")
@Composable
private fun updatePainter(painter: ImagePainter, request: ImageRequest) {
    painter.painter = if (LocalInspectionMode.current) {
        // If we're in inspection mode (preview) and we have a placeholder, just draw
        // that without executing an image request.
        request.placeholder?.toPainter()
    } else {
        // This may look like a useless remember, but this allows any Painter instances
        // to receive remember events (if it implements RememberObserver). Do not remove.
        remember(painter.state) { painter.state.painter }
    } ?: EmptyPainter
}

@SuppressLint("ComposableNaming")
@Composable
private fun updateFadeInTransition(painter: ImagePainter, durationMillis: Int) {
    // Short circuit if the fade in is not enabled.
    if (durationMillis <= 0) {
        painter.transitionColorFilter = null
        return
    }

    // Short circuit if the result is not successful or is from the memory cache.
    val state = painter.state
    if (state !is State.Success || state.metadata.dataSource == DataSource.MEMORY_CACHE) {
        painter.transitionColorFilter = null
        return
    }

    // Short circuit if the fade-in isn't running.
    val fadeInTransition = rememberFadeInTransition(state, durationMillis)
    if (fadeInTransition.isFinished) {
        painter.transitionColorFilter = null
        return
    }

    // Update the current color filter.
    val colorMatrix = remember { ColorMatrix() }
    colorMatrix.apply {
        updateAlpha(fadeInTransition.alpha)
        updateBrightness(fadeInTransition.brightness)
        updateSaturation(fadeInTransition.saturation)
    }
    painter.transitionColorFilter = ColorFilter.colorMatrix(colorMatrix)
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

/** A [Painter] that draws nothing and has no intrinsic size. */
private object EmptyPainter : Painter() {
    override val intrinsicSize: Size get() = Size.Unspecified
    override fun DrawScope.onDraw() {}
}
