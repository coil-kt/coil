package coil.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.times
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import coil.ImageLoader
import coil.compose.AsyncImagePainter.State
import coil.compose.AsyncImageScope.Companion.DefaultContent
import coil.request.ImageRequest
import coil.size.Dimension
import coil.size.Scale
import coil.size.SizeResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import coil.size.Size as CoilSize

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
 * @param alignment Optional alignment parameter used to place the [AsyncImagePainter] in the given
 *  bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *  used if the bounds are a different size from the intrinsic size of the [AsyncImagePainter].
 * @param alpha Optional opacity to be applied to the [AsyncImagePainter] when it is rendered
 *  onscreen.
 * @param colorFilter Optional [ColorFilter] to apply for the [AsyncImagePainter] when it is
 *  rendered onscreen.
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn
 *  into the destination.
 */
@Composable
fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    loading: @Composable (AsyncImageScope.(State.Loading) -> Unit)? = null,
    success: @Composable (AsyncImageScope.(State.Success) -> Unit)? = null,
    error: @Composable (AsyncImageScope.(State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
) = AsyncImage(
    model = model,
    contentDescription = contentDescription,
    imageLoader = imageLoader,
    modifier = modifier,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    filterQuality = filterQuality,
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
 * @param alignment Optional alignment parameter used to place the [AsyncImagePainter] in the given
 *  bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be
 *  used if the bounds are a different size from the intrinsic size of the [AsyncImagePainter].
 * @param alpha Optional opacity to be applied to the [AsyncImagePainter] when it is rendered
 *  onscreen.
 * @param colorFilter Optional [ColorFilter] to apply for the [AsyncImagePainter] when it is
 *  rendered onscreen.
 * @param filterQuality Sampling algorithm applied to a bitmap when it is scaled and drawn
 *  into the destination.
 * @param content A callback to draw the content for the current [AsyncImagePainter.State].
 */
@Composable
fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    content: @Composable (AsyncImageScope.(State) -> Unit) = DefaultContent,
) {
    // Create and execute the image request.
    val request = updateRequest(requestOf(model), contentScale)
    val painter = rememberAsyncImagePainter(request, imageLoader, filterQuality)

    // Avoid subcomposition by resolving the constraints using a layout modifier.
    val sizeResolver = request.sizeResolver
    val modifierWithConstraintsResolver = if (sizeResolver is ConstraintsSizeResolver) {
        modifier.then(sizeResolver)
    } else {
        modifier
    }

    // Draw the content.
    if (content === DefaultContent) {
        // Fast path: don't recompose for each AsyncImagePainter state change.
        Layout(
            modifier = modifierWithConstraintsResolver
                .contentDescription(contentDescription)
                .clipToBounds()
                .paint(
                    painter = painter,
                    alignment = alignment,
                    contentScale = contentScale,
                    alpha = alpha,
                    colorFilter = colorFilter
                ),
            measurePolicy = { _, constraints ->
                layout(constraints.minWidth, constraints.minHeight) {}
            }
        )
    } else {
        // Slow path: recompose for each AsyncImagePainter state change.
        Box(
            modifier = modifierWithConstraintsResolver,
            contentAlignment = alignment
        ) {
            RealAsyncImageScope(
                parentScope = this,
                painter = painter,
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            ).content(painter.state)
        }
    }
}

/**
 * A scope for the children of [AsyncImage].
 */
@LayoutScopeMarker
@Immutable
interface AsyncImageScope : BoxScope {

    /** The painter that is drawn by [AsyncImageContent]. */
    val painter: AsyncImagePainter

    /** The content description for [AsyncImageContent]. */
    val contentDescription: String?

    /** The default alignment for any composables drawn in this scope. */
    val alignment: Alignment

    /** The content scale for [AsyncImageContent]. */
    val contentScale: ContentScale

    /** The alpha for [AsyncImageContent]. */
    val alpha: Float

    /** The color filter for [AsyncImageContent]. */
    val colorFilter: ColorFilter?

    companion object {
        /**
         * The default content composable only draws [AsyncImageContent] for all
         * [AsyncImagePainter] states.
         */
        val DefaultContent: @Composable (AsyncImageScope.(State) -> Unit) = { AsyncImageContent() }
    }
}

/**
 * A composable that draws an [AsyncImage]'s content with its current attributes.
 *
 * @see AsyncImageScope
 */
@Composable
fun AsyncImageScope.AsyncImageContent(
    modifier: Modifier = Modifier,
    painter: Painter = this.painter,
    contentDescription: String? = this.contentDescription,
    alignment: Alignment = this.alignment,
    contentScale: ContentScale = this.contentScale,
    alpha: Float = this.alpha,
    colorFilter: ColorFilter? = this.colorFilter,
) {
    // Compute the intrinsic size of the content.
    /*val contentSize = computeContentSize(
        constraints = constraints,
        srcSize = painter.intrinsicSize,
        contentScale = contentScale
    )*/
    val contentSize = Size.Unspecified

    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = if (contentSize.isSpecified) {
            // Apply `modifier` second to allow overriding `contentSize`.
            Modifier
                .size(with(LocalDensity.current) { contentSize.toDpSize() })
                .then(modifier)
        } else {
            modifier
        },
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )
}

@Stable
private fun contentOf(
    loading: @Composable (AsyncImageScope.(State.Loading) -> Unit)?,
    success: @Composable (AsyncImageScope.(State.Success) -> Unit)?,
    error: @Composable (AsyncImageScope.(State.Error) -> Unit)?,
) = if (loading != null || success != null || error != null) {
    { state ->
        var draw = true
        when (state) {
            is State.Loading -> if (loading != null) loading(state).also { draw = false }
            is State.Success -> if (success != null) success(state).also { draw = false }
            is State.Error -> if (error != null) error(state).also { draw = false }
            is State.Empty -> {} // Skipped if rendering on the main thread.
        }
        if (draw) AsyncImageContent()
    }
} else {
    DefaultContent
}

private fun Modifier.contentDescription(contentDescription: String?): Modifier {
    return if (contentDescription != null) {
        semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        this
    }
}

@Composable
private fun updateRequest(request: ImageRequest, contentScale: ContentScale): ImageRequest {
    return request.newBuilder()
        .apply {
            if (request.defined.sizeResolver == null) {
                size(remember { ConstraintsSizeResolver() })
            }
            if (request.defined.scale == null) {
                scale(contentScale.toScale())
            }
        }
        .build()
}

@Stable
private fun computeContentSize(
    constraints: Constraints,
    srcSize: Size,
    contentScale: ContentScale
): Size {
    if (constraints.isZero || srcSize.isUnspecified) {
        return Size.Unspecified
    }

    // Only set a specific content size if at least one dimension is fixed.
    val hasFixedAndBoundedWidth = constraints.hasFixedWidth && constraints.hasBoundedWidth
    val hasFixedAndBoundedHeight = constraints.hasFixedHeight && constraints.hasBoundedHeight
    if (!hasFixedAndBoundedWidth && !hasFixedAndBoundedHeight) {
        return Size.Unspecified
    }

    val dstSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
    return srcSize * contentScale.computeScaleFactor(srcSize, dstSize)
}

@Stable
private fun ContentScale.toScale() = when (this) {
    ContentScale.Fit, ContentScale.Inside, ContentScale.None -> Scale.FIT
    else -> Scale.FILL
}

@Stable
private fun Constraints.toSizeOrNull() = when {
    isZero -> null
    else -> CoilSize(
        width = if (hasBoundedWidth) Dimension(maxWidth) else Dimension.Original,
        height = if (hasBoundedHeight) Dimension(maxHeight) else Dimension.Original
    )
}

private class ConstraintsSizeResolver : SizeResolver, LayoutModifier {

    private val constraintsFlow = MutableStateFlow(ZeroConstraints)

    override suspend fun size() = constraintsFlow.mapNotNull { it.toSizeOrNull() }.first()

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Set the current constraints.
        constraintsFlow.value = constraints

        // Measure and layout the content.
        val placeable = measurable.measure(constraints)
        return layout(
            width = constraints.maxWidth,
            height = constraints.maxHeight
        ) {
            placeable.place(0, 0)
        }
    }
}

private data class RealAsyncImageScope(
    val parentScope: BoxScope,
    override val painter: AsyncImagePainter,
    override val contentDescription: String?,
    override val alignment: Alignment,
    override val contentScale: ContentScale,
    override val alpha: Float,
    override val colorFilter: ColorFilter?,
) : AsyncImageScope, BoxScope by parentScope

private val ZeroConstraints = Constraints(0, 0, 0, 0)
