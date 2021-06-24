@file:Suppress("ComposableNaming", "unused")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun rememberImagePainter(
    data: Any?,
    imageLoader: ImageLoader = LocalImageLoader.current,
    onSizeChange: SizeChangeCallback = { _, _ -> false },
    fadeInMillis: Int = 0,
    builder: ImageRequest.Builder.() -> Unit = {},
): ImagePainter {
    val context = LocalContext.current
    val request = remember(context, data, builder) {
        ImageRequest.Builder(context)
            .data(data)
            .apply(builder)
            .build()
    }
    return rememberImagePainter(request, imageLoader, onSizeChange, fadeInMillis)
}

@Composable
fun rememberImagePainter(
    request: ImageRequest,
    imageLoader: ImageLoader = LocalImageLoader.current,
    onSizeChange: SizeChangeCallback = { _, _ -> false },
    fadeInMillis: Int = 0,
): ImagePainter {
    requireSupportedData(request.data)
    require(request.target == null) { "request.target must be null." }
    require(fadeInMillis >= 0) { "fadeInMillis must be >= 0."}

    val scope = rememberCoroutineScope { SupervisorJob() + Dispatchers.Main.immediate }
    val painter = remember(scope) { ImagePainter(scope, request, imageLoader) }
    painter.request = request
    painter.imageLoader = imageLoader
    painter.rootViewSize = LocalView.current.run { IntSize(width, height) }
    painter.onSizeChange = onSizeChange

    updatePainter(painter, request)
    animateFadeInTransition(painter, fadeInMillis)

    return painter
}

class ImagePainter internal constructor(
    private val scope: CoroutineScope,
    request: ImageRequest,
    imageLoader: ImageLoader
) : Painter(), RememberObserver {

    private var lazyPaint: Paint? = null
    private val paint: Paint get() = lazyPaint ?: Paint().also { lazyPaint = it }

    private var requestJob: Job? = null
    private var requestSize: IntSize by mutableStateOf(IntSize.Zero)

    private var alpha: Float by mutableStateOf(1f)
    private var colorFilter: ColorFilter? by mutableStateOf(null)

    internal var painter: Painter by mutableStateOf(EmptyPainter)
    internal var transitionColorFilter: ColorFilter? by mutableStateOf(null)
    internal var rootViewSize: IntSize by mutableStateOf(IntSize.Zero)
    internal var onSizeChange: SizeChangeCallback by mutableStateOf({ _, _ -> false })

    /** The current [ImagePainter.State]. */
    var state: State by mutableStateOf(State.Empty(request))
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
            // Else we just draw the content directly using the filter.
            drawPainter()
        }
    }

    private fun DrawScope.drawPainter() = with(painter) {
        draw(size, alpha, colorFilter ?: transitionColorFilter)
    }

    override fun onRemembered() {
        requestJob?.cancel()
        requestJob = scope.launch {
            combine(
                snapshotFlow { request },
                snapshotFlow { requestSize },
                transform = { request, size -> request to size }
            ).collectLatest { (request, size) -> execute(request, size) }
        }
    }

    override fun onForgotten() {
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
                rootViewSize.width > 0 -> rootViewSize.width
                else -> -1
            },
            height = when {
                // If we have a canvas height, use it...
                canvasSize.height >= 0.5f -> canvasSize.height.roundToInt()
                // Otherwise we fall-back to the root view size as an upper bound.
                rootViewSize.height > 0 -> rootViewSize.height
                else -> -1
            }
        )
    }

    private suspend fun execute(request: ImageRequest, size: IntSize) {
        if (size == IntSize.Zero) {
            state = State.Empty(request)
            return
        }

        if (state !is State.Empty && request == state.request && !onSizeChange(state, size)) {
            return
        }

        val newRequest = request.newBuilder()
            .target(
                onStart = { placeholder ->
                    state = State.Loading(
                        painter = placeholder?.toPainter(),
                        request = request
                    )
                }
            )
            .apply {
                if (request.defined.sizeResolver == null && size.width > 0 && size.height > 0) {
                    size(size.width, size.height)
                }
                if (request.defined.precision.let { it == null || it == Precision.AUTOMATIC }) {
                    precision(Precision.INEXACT)
                }
            }
            .build()
        state = imageLoader.execute(newRequest).toState(request)
    }

    private fun ImageResult.toState(request: ImageRequest): State {
        return when (this) {
            is SuccessResult -> {
                State.Success(
                    painter = drawable.toPainter(),
                    request = request,
                    metadata = metadata
                )
            }
            is ErrorResult -> {
                State.Error(
                    painter = drawable?.toPainter(),
                    request = request,
                    throwable = throwable
                )
            }
        }
    }

    sealed class State {

        abstract val painter: Painter?
        abstract val request: ImageRequest

        data class Empty(
            override val request: ImageRequest
        ) : State() {
            override val painter: Painter? get() = null
        }

        data class Loading(
            override val painter: Painter?,
            override val request: ImageRequest
        ) : State()

        data class Success(
            override val painter: Painter,
            override val request: ImageRequest,
            val metadata: ImageResult.Metadata
        ) : State()

        data class Error(
            override val painter: Painter?,
            override val request: ImageRequest,
            val throwable: Throwable
        ) : State()
    }
}

/**
 * Interface that allows apps to control whether a request is re-run once the size changes.
 * Return `true` if the request should be re-run if the size has changed.
 */
typealias SizeChangeCallback = (state: ImagePainter.State, size: IntSize) -> Boolean

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

/**
 * Allows us observe the current result [Painter] as state. This function allows us to
 * minimize the amount of composition needed such that this function only needs to be restarted
 * when the `ImagePainter.state` changes.
 */
@Composable
private fun updatePainter(painter: ImagePainter, request: ImageRequest) {
    painter.painter = if (LocalInspectionMode.current) {
        // If we're in inspection mode (preview) and we have a placeholder, just draw
        // that without executing an image request.
        request.placeholder?.toPainter() ?: EmptyPainter
    } else {
        // This may look like a useless remember, but this allows any Painter instances
        // to receive remember events (if it implements RememberObserver). Do not remove.
        remember(painter.state) { painter.state.painter } ?: EmptyPainter
    }
}

@Composable
private fun animateFadeInTransition(painter: ImagePainter, durationMillis: Int) {
    // Short circuit if the fade in is not enabled.
    if (durationMillis <= 0) {
        painter.transitionColorFilter = null
        return
    }

    // Short circuit if the result is not successful or is from the memory cache.
    val state = painter.state
    if (state !is ImagePainter.State.Success || state.metadata.dataSource == DataSource.MEMORY_CACHE) {
        painter.transitionColorFilter = null
        return
    }

    // Short circuit if the fade-in isn't running.
    val fadeInTransition = updateFadeInTransition(state, durationMillis)
    if (fadeInTransition.isFinished) {
        painter.transitionColorFilter = null
        return
    }

    val colorMatrix = remember { ColorMatrix() }
    colorMatrix.apply {
        updateAlpha(fadeInTransition.alpha)
        updateBrightness(fadeInTransition.brightness)
        updateSaturation(fadeInTransition.saturation)
    }
    painter.transitionColorFilter = ColorFilter.colorMatrix(colorMatrix)
}

/** A [Painter] that draws nothing and has no intrinsic size. */
private object EmptyPainter : Painter() {
    override val intrinsicSize: Size get() = Size.Unspecified
    override fun DrawScope.onDraw() {}
}
