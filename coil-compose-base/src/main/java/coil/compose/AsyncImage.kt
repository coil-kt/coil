package coil.compose

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Constraints.Companion.Infinity
import coil.ImageLoader
import coil.compose.AsyncImagePainter.State
import coil.compose.AsyncImageScope.Companion.DefaultContent
import coil.decode.DecodeUtils
import coil.request.ImageRequest
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.SizeResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
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

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = alignment
    ) {
        // Resolve the size for the image request.
        (request.sizeResolver as? ConstraintsSizeResolver)?.setConstraints(constraints)

        // Draw the content.
        RealAsyncImageScope(
            parent = this,
            contentSize = computeContentSize(constraints, painter.intrinsicSize),
            painter = painter,
            contentDescription = contentDescription,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter
        ).content(painter.state)
    }
}

/**
 * A scope for the children of [AsyncImage].
 */
@LayoutScopeMarker
@Immutable
interface AsyncImageScope : BoxScope {

    val contentSize: Size

    val painter: AsyncImagePainter

    val contentDescription: String?

    val alignment: Alignment

    val contentScale: ContentScale

    val alpha: Float

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
) = Image(
    painter = painter,
    contentDescription = contentDescription,
    modifier = Modifier
        .apply {
            if (contentSize.isSpecified) {
                with(LocalDensity.current) {
                    size(contentSize.toDpSize())
                }
            }
        }
        .then(modifier),
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter
)

@Stable
private fun contentOf(
    loading: @Composable (AsyncImageScope.(State.Loading) -> Unit)?,
    success: @Composable (AsyncImageScope.(State.Success) -> Unit)?,
    error: @Composable (AsyncImageScope.(State.Error) -> Unit)?,
) = if (loading != null && success != null && error != null) {
    DefaultContent
} else {
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
}

@Composable
private fun updateRequest(request: ImageRequest, contentScale: ContentScale): ImageRequest {
    return request.newBuilder()
        .apply {
            if (request.defined.sizeResolver == null) {
                val context = LocalContext.current
                val sizeResolver = remember(context) { ConstraintsSizeResolver(context) }
                size(sizeResolver)
            }
            if (request.defined.scale == null) {
                scale(contentScale.toScale())
            }
        }
        .build()
}

@Stable
private fun computeContentSize(constraints: Constraints, intrinsicSize: Size): Size {
    if (intrinsicSize.isUnspecified) {
        return Size.Unspecified
    }

    val minWidth = constraints.minWidth
    val minHeight = constraints.minHeight
    if (minWidth == Infinity || minHeight == Infinity) {
        return Size.Unspecified
    }

    val srcWidth = intrinsicSize.width
    val srcHeight = intrinsicSize.height
    val scale = DecodeUtils.computeSizeMultiplier(
        srcWidth = srcWidth,
        srcHeight = srcHeight,
        dstWidth = minWidth.toFloat(),
        dstHeight = minHeight.toFloat(),
        scale = Scale.FILL
    ).coerceAtLeast(1f)
    return constraints.constrain(
        width = scale * srcWidth,
        height = scale * srcHeight
    )
}

@Stable
private fun ContentScale.toScale() = when (this) {
    ContentScale.Fit, ContentScale.Inside, ContentScale.None -> Scale.FIT
    else -> Scale.FILL
}

@Stable
private fun Constraints.constrain(width: Float, height: Float) = Size(
    width = width.coerceIn(minWidth.toFloat(), maxWidth.toFloat()),
    height = height.coerceIn(minHeight.toFloat(), maxHeight.toFloat())
)

private class ConstraintsSizeResolver(private val context: Context) : SizeResolver {

    private val constraints = MutableStateFlow<Constraints?>(null)

    override suspend fun size() = constraints.filterNotNull().first().toSize()

    fun setConstraints(constraints: Constraints) {
        this.constraints.value = constraints
    }

    private fun Constraints.toSize(): CoilSize {
        if (isZero) return OriginalSize

        val hasBoundedWidth = hasBoundedWidth
        val hasBoundedHeight = hasBoundedHeight
        return when {
            hasBoundedWidth && hasBoundedHeight -> PixelSize(maxWidth, maxHeight)
            hasBoundedWidth -> PixelSize(maxWidth, context.resources.displayMetrics.heightPixels)
            hasBoundedHeight -> PixelSize(context.resources.displayMetrics.widthPixels, maxHeight)
            else -> OriginalSize
        }
    }
}

private data class RealAsyncImageScope(
    private val parent: BoxScope,
    override val contentSize: Size,
    override val painter: AsyncImagePainter,
    override val contentDescription: String?,
    override val alignment: Alignment,
    override val contentScale: ContentScale,
    override val alpha: Float,
    override val colorFilter: ColorFilter?,
) : AsyncImageScope, BoxScope by parent
