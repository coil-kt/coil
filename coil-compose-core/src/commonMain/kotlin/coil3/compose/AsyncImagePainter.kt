package coil3.compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.util.trace
import coil3.Image
import coil3.ImageLoader
import coil3.annotation.Poko
import coil3.compose.AsyncImagePainter.Companion.DefaultTransform
import coil3.compose.AsyncImagePainter.Input
import coil3.compose.AsyncImagePainter.State
import coil3.compose.internal.AsyncImageState
import coil3.compose.internal.onStateOf
import coil3.compose.internal.rememberImmediateCoroutineScope
import coil3.compose.internal.requestOf
import coil3.compose.internal.toScale
import coil3.compose.internal.transformOf
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Precision
import coil3.size.SizeResolver
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch

/**
 * Return an [AsyncImagePainter] that executes an [ImageRequest] asynchronously and renders the result.
 *
 * @param model Either an [ImageRequest] or the [ImageRequest.data] value.
 * @param imageLoader The [ImageLoader] that will be used to execute the request.
 * @param placeholder A [Painter] that is displayed while the image is loading.
 * @param error A [Painter] that is displayed when the image request is unsuccessful.
 * @param fallback A [Painter] that is displayed when the request's [ImageRequest.data] is null.
 * @param onLoading Called when the image request begins loading.
 * @param onSuccess Called when the image request completes successfully.
 * @param onError Called when the image request completes unsuccessfully.
 * @param contentScale Used to determine the aspect ratio scaling to be used if the canvas bounds
 *  are a different size from the intrinsic size of the image loaded by [model]. This should be set
 *  to the same value that's passed to [Image].
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn into the
 *  destination.
 */
@Composable
@NonRestartableComposable
fun rememberAsyncImagePainter(
    model: Any?,
    imageLoader: ImageLoader,
    placeholder: Painter? = null,
    error: Painter? = null,
    fallback: Painter? = error,
    onLoading: ((State.Loading) -> Unit)? = null,
    onSuccess: ((State.Success) -> Unit)? = null,
    onError: ((State.Error) -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Fit,
    filterQuality: FilterQuality = DefaultFilterQuality,
) = rememberAsyncImagePainter(
    state = AsyncImageState(model, imageLoader),
    transform = transformOf(placeholder, error, fallback),
    onState = onStateOf(onLoading, onSuccess, onError),
    contentScale = contentScale,
    filterQuality = filterQuality,
)

/**
 * Return an [AsyncImagePainter] that executes an [ImageRequest] asynchronously and renders the result.
 *
 * @param model Either an [ImageRequest] or the [ImageRequest.data] value.
 * @param imageLoader The [ImageLoader] that will be used to execute the request.
 * @param transform A callback to transform a new [State] before it's applied to the
 *  [AsyncImagePainter]. Typically this is used to overwrite the state's [Painter].
 * @param onState Called when the state of this painter changes.
 * @param contentScale Used to determine the aspect ratio scaling to be used if the canvas bounds
 *  are a different size from the intrinsic size of the image loaded by [model]. This should be set
 *  to the same value that's passed to [Image].
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn into the
 *  destination.
 */
@Composable
@NonRestartableComposable
fun rememberAsyncImagePainter(
    model: Any?,
    imageLoader: ImageLoader,
    transform: (State) -> State = DefaultTransform,
    onState: ((State) -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Fit,
    filterQuality: FilterQuality = DefaultFilterQuality,
) = rememberAsyncImagePainter(
    state = AsyncImageState(model, imageLoader),
    transform = transform,
    onState = onState,
    contentScale = contentScale,
    filterQuality = filterQuality,
)

@Composable
private fun rememberAsyncImagePainter(
    state: AsyncImageState,
    transform: (State) -> State,
    onState: ((State) -> Unit)?,
    contentScale: ContentScale,
    filterQuality: FilterQuality,
): AsyncImagePainter = trace("rememberAsyncImagePainter") {
    val request = requestOf(state.model)
    validateRequest(request)

    val input = Input(state.imageLoader, request, state.modelEqualityDelegate)
    val painter = remember { AsyncImagePainter(input) }
    painter.scope = rememberImmediateCoroutineScope()
    painter.transform = transform
    painter.onState = onState
    painter.contentScale = contentScale
    painter.filterQuality = filterQuality
    painter.previewHandler = previewHandler()
    painter._input.value = input
    return painter
}

/**
 * A [Painter] that that executes an [ImageRequest] asynchronously and renders the [ImageResult].
 */
@Stable
class AsyncImagePainter internal constructor(
    input: Input,
) : Painter(), RememberObserver {
    private val drawSize = MutableSharedFlow<Size>(
        replay = 1,
        onBufferOverflow = DROP_OLDEST,
    )
    private val restartSignal = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    private var painter: Painter? by mutableStateOf(null)
    private var alpha: Float by mutableFloatStateOf(DefaultAlpha)
    private var colorFilter: ColorFilter? by mutableStateOf(null)

    private var rememberJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }

    internal lateinit var scope: CoroutineScope
    internal var transform = DefaultTransform
    internal var onState: ((State) -> Unit)? = null
    internal var contentScale = ContentScale.Fit
    internal var filterQuality = DefaultFilterQuality
    internal var previewHandler: AsyncImagePreviewHandler? = null

    /** The latest [AsyncImagePainter.Input]. */
    internal val _input: MutableStateFlow<Input> = MutableStateFlow(input)
    val input: StateFlow<Input> = _input.asStateFlow()

    /** The latest [AsyncImagePainter.State]. */
    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Empty)
    val state: StateFlow<State> = _state.asStateFlow()

    override val intrinsicSize: Size
        get() = painter?.intrinsicSize ?: Size.Unspecified

    override fun DrawScope.onDraw() {
        // Cache the draw scope's current size.
        drawSize.tryEmit(size)

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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onRemembered() = trace("AsyncImagePainter.onRemembered") {
        (painter as? RememberObserver)?.onRemembered()

        // Observe the latest request and execute any emissions.
        rememberJob = scope.launch {
            restartSignal
                .flatMapLatest { _input }
                .mapLatest { input ->
                    val previewHandler = previewHandler
                    if (previewHandler != null) {
                        // If we're in inspection mode use the preview renderer.
                        val request = updateRequest(input.request, isPreview = true)
                        previewHandler.handle(input.imageLoader, request)
                    } else {
                        // Else, execute the request as normal.
                        val request = updateRequest(input.request, isPreview = false)
                        input.imageLoader.execute(request).toState()
                    }
                }
                .collect(::updateState)
        }
    }

    override fun onForgotten() {
        rememberJob = null
        (painter as? RememberObserver)?.onForgotten()
    }

    override fun onAbandoned() {
        rememberJob = null
        (painter as? RememberObserver)?.onAbandoned()
    }

    /**
     * Launch a new image request with the current [Input]s.
     */
    fun restart() {
        restartSignal.tryEmit(Unit)
    }

    /**
     * Update the [request] to work with [AsyncImagePainter].
     */
    private fun updateRequest(request: ImageRequest, isPreview: Boolean): ImageRequest {
        // Connect the size resolver to the draw scope if necessary.
        val sizeResolver = request.sizeResolver
        if (sizeResolver is DrawScopeSizeResolver) {
            sizeResolver.connect(drawSize)
        }

        return request.newBuilder()
            .target(
                onStart = { placeholder ->
                    val painter = placeholder?.asPainter(request.context, filterQuality)
                    updateState(State.Loading(painter))
                },
            )
            .apply {
                if (request.defined.sizeResolver == null) {
                    // If the size resolver isn't set, use the original size.
                    size(SizeResolver.ORIGINAL)
                }
                if (request.defined.scale == null) {
                    // If the scale isn't set, use the content scale.
                    scale(contentScale.toScale())
                }
                if (request.defined.precision == null) {
                    // AsyncImagePainter scales the image to fit the canvas size at draw time.
                    precision(Precision.INEXACT)
                }
                if (isPreview) {
                    // The request must be executed synchronously in the preview environment.
                    coroutineContext(EmptyCoroutineContext)
                }
            }
            .build()
    }

    private fun updateState(state: State) {
        val previous = _state.value
        val current = transform(state)
        _state.value = current
        painter = maybeNewCrossfadePainter(previous, current, contentScale) ?: current.painter

        // Manually forget and remember the old/new painters.
        if (previous.painter !== current.painter) {
            (previous.painter as? RememberObserver)?.onForgotten()
            (current.painter as? RememberObserver)?.onRemembered()
        }

        // Notify the state listener.
        onState?.invoke(current)
    }

    private fun ImageResult.toState() = when (this) {
        is SuccessResult -> State.Success(
            painter = image.asPainter(request.context, filterQuality),
            result = this,
        )
        is ErrorResult -> State.Error(
            painter = image?.asPainter(request.context, filterQuality),
            result = this,
        )
    }

    /**
     * The latest arguments passed to [AsyncImagePainter].
     */
    @Poko
    class Input(
        val imageLoader: ImageLoader,
        val request: ImageRequest,
        val modelEqualityDelegate: AsyncImageModelEqualityDelegate,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Input &&
                imageLoader == other.imageLoader &&
                modelEqualityDelegate == other.modelEqualityDelegate &&
                modelEqualityDelegate.equals(request, other.request)
        }

        override fun hashCode(): Int {
            var result = imageLoader.hashCode()
            result = 31 * result + modelEqualityDelegate.hashCode()
            result = 31 * result + modelEqualityDelegate.hashCode(request)
            return result
        }
    }

    /**
     * The current state of the [AsyncImagePainter].
     */
    sealed interface State {

        /** The current painter being drawn by [AsyncImagePainter]. */
        val painter: Painter?

        /** The request has not been started. */
        data object Empty : State {
            override val painter: Painter? get() = null
        }

        /** The request is in-progress. */
        data class Loading(
            override val painter: Painter?,
        ) : State

        /** The request was successful. */
        data class Success(
            override val painter: Painter,
            val result: SuccessResult,
        ) : State

        /** The request failed due to [ErrorResult.throwable]. */
        data class Error(
            override val painter: Painter?,
            val result: ErrorResult,
        ) : State
    }

    companion object {
        /**
         * A state transform that does not modify the state.
         */
        val DefaultTransform: (State) -> State = { it }
    }
}

@ReadOnlyComposable
@Composable
private fun previewHandler(): AsyncImagePreviewHandler? {
    return if (LocalInspectionMode.current) {
        LocalAsyncImagePreviewHandler.current
    } else {
        null
    }
}

private fun validateRequest(request: ImageRequest) {
    when (request.data) {
        is ImageRequest.Builder -> unsupportedData(
            name = "ImageRequest.Builder",
            description = "Did you forget to call ImageRequest.Builder.build()?",
        )
        is ImageBitmap -> unsupportedData("ImageBitmap")
        is ImageVector -> unsupportedData("ImageVector")
        is Painter -> unsupportedData("Painter")
    }
    validateRequestProperties(request)
}

private fun unsupportedData(
    name: String,
    description: String = "If you wish to display this $name, use androidx.compose.foundation.Image.",
): Nothing = throw IllegalArgumentException("Unsupported type: $name. $description")

/** Validate platform-specific properties of an [ImageRequest]. */
internal expect fun validateRequestProperties(request: ImageRequest)

/** Create and return a [CrossfadePainter] if requested. */
internal expect fun maybeNewCrossfadePainter(
    previous: State,
    current: State,
    contentScale: ContentScale,
): CrossfadePainter?
