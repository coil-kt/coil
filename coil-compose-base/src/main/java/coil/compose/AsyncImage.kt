package coil.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
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
    placeholder: Painter? = null,
    error: Painter? = null,
    fallback: Painter? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
    content: @Composable (AsyncImageScope.() -> Unit) = DefaultContent,
) {
    // Create and execute the image request.
    val request = updateRequest(requestOf(model), contentScale, placeholder, error, fallback)
    val painter = rememberAsyncImagePainter(request, imageLoader, filterQuality)

    val sizeResolver = request.sizeResolver
    if (content === DefaultContent) {
        // Fastest path: draw the content without parent composables or subcomposition.
        InternalAsyncImageContent(
            modifier = if (sizeResolver is ConstraintsSizeResolver) {
                modifier.then(sizeResolver)
            } else {
                modifier
            },
            painter = painter,
            contentDescription = contentDescription,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter
        )
    } else if (sizeResolver !is ConstraintsSizeResolver) {
        // Fast path: draw the content inside a parent composable without subcomposition.
        Box(
            contentAlignment = alignment,
            propagateMinConstraints = true
        ) {
            RealAsyncImageScope(
                parentScope = this,
                painter = painter,
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            ).content()
        }
    } else {
        // Slow path: draw the content inside a parent composable with subcomposition.
        BoxWithConstraints(
            contentAlignment = alignment,
            propagateMinConstraints = true
        ) {
            // This is necessary to ensure that images from the memory cache are resolved
            // synchronously before invoking `content`.
            sizeResolver.setConstraints(constraints)

            RealAsyncImageScope(
                parentScope = this,
                painter = painter,
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            ).content()
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
         * The default content composable only draws [AsyncImageContent].
         */
        val DefaultContent: @Composable (AsyncImageScope.() -> Unit) = { AsyncImageContent() }
    }
}

/**
 * A composable that draws [AsyncImage]'s content with [AsyncImageScope]'s properties.
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
) = InternalAsyncImageContent(
    modifier = modifier,
    painter = painter,
    contentDescription = contentDescription,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter
)

/** Draws the current content without an [AsyncImageScope]. */
@Composable
private fun InternalAsyncImageContent(
    modifier: Modifier,
    painter: Painter,
    contentDescription: String?,
    alignment: Alignment,
    contentScale: ContentScale,
    alpha: Float,
    colorFilter: ColorFilter?,
) = Layout(
    modifier = modifier
        .contentDescription(contentDescription)
        .clipToBounds()
        .then(
            ContentPainterModifier(
                painter = painter,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            )
        ),
    measurePolicy = { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {}
    }
)

@Stable
private fun contentOf(
    loading: @Composable (AsyncImageScope.(State.Loading) -> Unit)?,
    success: @Composable (AsyncImageScope.(State.Success) -> Unit)?,
    error: @Composable (AsyncImageScope.(State.Error) -> Unit)?,
) = if (loading != null || success != null || error != null) {
    {
        var draw = true
        when (val state = painter.state) {
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

@Composable
private fun updateRequest(
    request: ImageRequest,
    contentScale: ContentScale,
    placeholder: Painter?,
    error: Painter?,
    fallback: Painter?
): ImageRequest {
    return request.newBuilder()
        .apply {
            if (request.defined.sizeResolver == null) {
                size(remember { ConstraintsSizeResolver() })
            }
            if (request.defined.scale == null) {
                scale(contentScale.toScale())
            }
            placeholder(placeholder)
            error(error)
            fallback(fallback)
        }
        .build()
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

private class ConstraintsSizeResolver : SizeResolver, LayoutModifier {

    private val _constraints = MutableStateFlow(ZeroConstraints)

    override suspend fun size() = _constraints.mapNotNull { it.toSizeOrNull() }.first()

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Cache the current constraints.
        _constraints.value = constraints

        // Measure and layout the content.
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }

    fun setConstraints(constraints: Constraints) {
        _constraints.value = constraints
    }
}

private data class RealAsyncImageScope(
    private val parentScope: BoxScope,
    override val painter: AsyncImagePainter,
    override val contentDescription: String?,
    override val alignment: Alignment,
    override val contentScale: ContentScale,
    override val alpha: Float,
    override val colorFilter: ColorFilter?,
) : AsyncImageScope, BoxScope by parentScope

private val ZeroConstraints = Constraints.fixed(0, 0)
