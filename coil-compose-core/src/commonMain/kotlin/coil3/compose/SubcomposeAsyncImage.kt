@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import coil3.ImageLoader
import coil3.annotation.Data
import coil3.compose.AsyncImagePainter.Companion.DefaultTransform
import coil3.compose.AsyncImagePainter.State
import coil3.compose.internal.AsyncImageState
import coil3.compose.internal.ConstraintsSizeResolver
import coil3.compose.internal.ContentPainterElement
import coil3.compose.internal.contentDescription
import coil3.compose.internal.onStateOf
import coil3.compose.internal.requestOfWithSizeResolver
import coil3.request.ImageRequest

/**
 * A composable that executes an [ImageRequest] asynchronously and renders the result.
 *
 * @param model Either an [ImageRequest] or the [ImageRequest.data] value.
 * @param contentDescription Text used by accessibility services to describe what this image
 *  represents. This should always be provided unless this image is used for decorative purposes,
 *  and does not represent a meaningful action that a user can take.
 * @param imageLoader The [ImageLoader] that will be used to execute the request.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content.
 * @param loading An optional callback to overwrite what's drawn while the image request is loading.
 * @param success An optional callback to overwrite what's drawn when the image request succeeds.
 * @param error An optional callback to overwrite what's drawn when the image request fails.
 * @param onLoading Called when the image request begins loading.
 * @param onSuccess Called when the image request completes successfully.
 * @param onError Called when the image request completes unsuccessfully.
 * @param alignment Optional alignment parameter used to place the [AsyncImagePainter] in the given
 *  bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *  used if the bounds are a different size from the intrinsic size of the [AsyncImagePainter].
 * @param alpha Optional opacity to be applied to the [AsyncImagePainter] when it is rendered
 *  onscreen.
 * @param colorFilter Optional [ColorFilter] to apply for the [AsyncImagePainter] when it is
 *  rendered onscreen.
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn into the
 *  destination.
 * @param clipToBounds If true, clips the content to its bounds. Else, it will not be clipped.
 * @param modelEqualityDelegate Determines the equality of [model]. This controls whether this
 *  composable is redrawn and a new image request is launched when the outer composable recomposes.
 */
@Composable
@NonRestartableComposable
fun SubcomposeAsyncImage(
    model: Any?,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    transform: (State) -> State = DefaultTransform,
    loading: @Composable (SubcomposeAsyncImageScope.(State.Loading) -> Unit)? = null,
    success: @Composable (SubcomposeAsyncImageScope.(State.Success) -> Unit)? = null,
    error: @Composable (SubcomposeAsyncImageScope.(State.Error) -> Unit)? = null,
    onLoading: ((State.Loading) -> Unit)? = null,
    onSuccess: ((State.Success) -> Unit)? = null,
    onError: ((State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    clipToBounds: Boolean = true,
    modelEqualityDelegate: EqualityDelegate = DefaultModelEqualityDelegate,
) = SubcomposeAsyncImage(
    state = AsyncImageState(model, modelEqualityDelegate, imageLoader),
    contentDescription = contentDescription,
    modifier = modifier,
    transform = transform,
    onState = onStateOf(onLoading, onSuccess, onError),
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    filterQuality = filterQuality,
    clipToBounds = clipToBounds,
    content = contentOf(loading, success, error),
)

/**
 * A composable that executes an [ImageRequest] asynchronously and renders the result.
 *
 * @param model Either an [ImageRequest] or the [ImageRequest.data] value.
 * @param contentDescription Text used by accessibility services to describe what this image
 *  represents. This should always be provided unless this image is used for decorative purposes,
 *  and does not represent a meaningful action that a user can take.
 * @param imageLoader The [ImageLoader] that will be used to execute the request.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content.
 * @param transform A callback to transform a new [State] before it's applied to the
 *  [AsyncImagePainter]. Typically this is used to modify the state's [Painter].
 * @param onState Called when the state of this painter changes.
 * @param alignment Optional alignment parameter used to place the [AsyncImagePainter] in the given
 *  bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *  used if the bounds are a different size from the intrinsic size of the [AsyncImagePainter].
 * @param alpha Optional opacity to be applied to the [AsyncImagePainter] when it is rendered
 *  onscreen.
 * @param colorFilter Optional [ColorFilter] to apply for the [AsyncImagePainter] when it is
 *  rendered onscreen.
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn into the
 *  destination.
 * @param clipToBounds If true, clips the content to its bounds. Else, it will not be clipped.
 * @param modelEqualityDelegate Determines the equality of [model]. This controls whether this
 *  composable is redrawn and a new image request is launched when the outer composable recomposes.
 * @param content A callback to draw the content inside a [SubcomposeAsyncImageScope].
 */
@Composable
@NonRestartableComposable
fun SubcomposeAsyncImage(
    model: Any?,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    transform: (State) -> State = DefaultTransform,
    onState: ((State) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    clipToBounds: Boolean = true,
    modelEqualityDelegate: EqualityDelegate = DefaultModelEqualityDelegate,
    content: @Composable SubcomposeAsyncImageScope.() -> Unit,
) = SubcomposeAsyncImage(
    state = AsyncImageState(model, modelEqualityDelegate, imageLoader),
    contentDescription = contentDescription,
    modifier = modifier,
    transform = transform,
    onState = onState,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    filterQuality = filterQuality,
    clipToBounds = clipToBounds,
    content = content,
)

@Composable
private fun SubcomposeAsyncImage(
    state: AsyncImageState,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    transform: (State) -> State,
    onState: ((State) -> Unit)?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
    filterQuality: FilterQuality,
    clipToBounds: Boolean,
    content: @Composable SubcomposeAsyncImageScope.() -> Unit,
) {
    // Create and execute the image request.
    val request = requestOfWithSizeResolver(
        model = state.model,
        contentScale = contentScale,
    )
    val painter = rememberAsyncImagePainter(
        model = request,
        imageLoader = state.imageLoader,
        transform = transform,
        onState = onState,
        contentScale = contentScale,
        filterQuality = filterQuality,
    )

    val sizeResolver = request.sizeResolver
    if (sizeResolver !is ConstraintsSizeResolver) {
        // Fast path: draw the content without subcomposition as we don't need to resolve the
        // constraints.
        Box(
            modifier = modifier,
            contentAlignment = alignment,
            propagateMinConstraints = true,
        ) {
            RealSubcomposeAsyncImageScope(
                parentScope = this,
                painter = painter,
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
                clipToBounds = clipToBounds,
            ).content()
        }
    } else {
        // Slow path: draw the content with subcomposition as we need to resolve the constraints
        // before calling `content`.
        BoxWithConstraints(
            modifier = modifier,
            contentAlignment = alignment,
            propagateMinConstraints = true,
        ) {
            // Ensure `painter.state` is up to date immediately. Resolving the constraints
            // synchronously is necessary to ensure that images from the memory cache are resolved
            // and `painter.state` is updated to `Success` before invoking `content`.
            sizeResolver.setConstraints(constraints)

            RealSubcomposeAsyncImageScope(
                parentScope = this,
                painter = painter,
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
                clipToBounds = clipToBounds,
            ).content()
        }
    }
}

/**
 * A scope for the children of [SubcomposeAsyncImage].
 */
@LayoutScopeMarker
@Immutable
interface SubcomposeAsyncImageScope : BoxScope {

    /** The painter that is drawn by [SubcomposeAsyncImageContent]. */
    val painter: AsyncImagePainter

    /** The content description for [SubcomposeAsyncImageContent]. */
    val contentDescription: String?

    /** The default alignment for any composables drawn in this scope. */
    val alignment: Alignment

    /** The content scale for [SubcomposeAsyncImageContent]. */
    val contentScale: ContentScale

    /** The alpha for [SubcomposeAsyncImageContent]. */
    val alpha: Float

    /** The color filter for [SubcomposeAsyncImageContent]. */
    val colorFilter: ColorFilter?

    /** If true, applies [clipToBounds] to [SubcomposeAsyncImageContent]. */
    val clipToBounds: Boolean
}

/**
 * A composable that draws [SubcomposeAsyncImage]'s content with [SubcomposeAsyncImageScope]'s
 * properties.
 *
 * @see SubcomposeAsyncImageScope
 */
@Composable
fun SubcomposeAsyncImageScope.SubcomposeAsyncImageContent(
    modifier: Modifier = Modifier,
    painter: Painter = this.painter,
    contentDescription: String? = this.contentDescription,
    alignment: Alignment = this.alignment,
    contentScale: ContentScale = this.contentScale,
    alpha: Float = this.alpha,
    colorFilter: ColorFilter? = this.colorFilter,
    clipToBounds: Boolean = this.clipToBounds,
) = Layout(
    modifier = modifier
        .contentDescription(contentDescription)
        .run { if (clipToBounds) clipToBounds() else this }
        .then(
            ContentPainterElement(
                painter = painter,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
            ),
        ),
    measurePolicy = { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    },
)

@Stable
private fun contentOf(
    loading: @Composable (SubcomposeAsyncImageScope.(State.Loading) -> Unit)?,
    success: @Composable (SubcomposeAsyncImageScope.(State.Success) -> Unit)?,
    error: @Composable (SubcomposeAsyncImageScope.(State.Error) -> Unit)?,
): @Composable SubcomposeAsyncImageScope.() -> Unit {
    return if (loading != null || success != null || error != null) {
        {
            var draw = true
            when (val state = painter.state) {
                is State.Loading -> if (loading != null) loading(state).also { draw = false }
                is State.Success -> if (success != null) success(state).also { draw = false }
                is State.Error -> if (error != null) error(state).also { draw = false }
                is State.Empty -> {} // Skipped if rendering on the main thread.
            }
            if (draw) SubcomposeAsyncImageContent()
        }
    } else {
        { SubcomposeAsyncImageContent() }
    }
}

@Data
private class RealSubcomposeAsyncImageScope(
    private val parentScope: BoxScope,
    override val painter: AsyncImagePainter,
    override val contentDescription: String?,
    override val alignment: Alignment,
    override val contentScale: ContentScale,
    override val alpha: Float,
    override val colorFilter: ColorFilter?,
    override val clipToBounds: Boolean,
) : SubcomposeAsyncImageScope, BoxScope by parentScope
