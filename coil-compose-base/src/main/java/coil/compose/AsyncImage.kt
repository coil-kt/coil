package coil.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter.State
import coil.request.ImageRequest
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/**
 * A composable that executes the given [ImageRequest] asynchronously and renders the resulting [ImagePainter].
 *
 * @param data The [ImageRequest.data] to load.
 * @param contentDescription Text used by accessibility services to describe what this image represents.
 * @param imageLoader The [ImageLoader] that will be used to execute the request.
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex. background).
 * @param alignment Optional alignment parameter used to place the [ImagePainter] in the given
 *  bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 *  if the bounds are a different size from the intrinsic size of the [ImagePainter].
 * @param alpha Optional opacity to be applied to the [ImagePainter] when it is rendered onscreen.
 * @param colorFilter Optional [ColorFilter] to apply for the [ImagePainter] when it is rendered onscreen.
 */
@ExperimentalCoilApi
@Composable
fun AsyncImage(
    data: Any?,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    loading: @Composable ((State.Loading) -> Unit)? = null,
    success: @Composable ((State.Success) -> Unit)? = null,
    error: @Composable ((State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) = AsyncImage(
    request = ImageRequest.Builder(LocalContext.current).data(data).build(),
    contentDescription = contentDescription,
    imageLoader = imageLoader,
    modifier = modifier,
    loading = loading,
    success = success,
    error = error,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
)

/**
 * A composable that executes the given [ImageRequest] asynchronously and renders the resulting [ImagePainter].
 *
 * @param request The [ImageRequest] to execute.
 * @param contentDescription Text used by accessibility services to describe what this image represents.
 * @param imageLoader The [ImageLoader] that will be used to execute [request].
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex. background).
 * @param alignment Optional alignment parameter used to place the [ImagePainter] in the given
 *  bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 *  if the bounds are a different size from the intrinsic size of the [ImagePainter].
 * @param alpha Optional opacity to be applied to the [ImagePainter] when it is rendered onscreen.
 * @param colorFilter Optional [ColorFilter] to apply for the [ImagePainter] when it is rendered onscreen.
 */
@ExperimentalCoilApi
@Composable
fun AsyncImage(
    request: ImageRequest,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    loading: @Composable ((State.Loading) -> Unit)? = null,
    success: @Composable ((State.Success) -> Unit)? = null,
    error: @Composable ((State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    val requestBuilder = request.newBuilder()
    val measureSize: MutableStateFlow<Size?>?
    if (request.defined.sizeResolver == null) {
        measureSize = MutableStateFlow(null)
        requestBuilder.size { measureSize.filterNotNull().first() }
    } else {
        measureSize = null
    }
    if (request.defined.scale == null) {
        requestBuilder.scale(contentScale.toScale())
    }
    val painter = rememberImagePainter(requestBuilder.build(), imageLoader)

    // Support overriding what's drawn for each image painter state.
    if (loading != null || success != null || error != null) {
        when (val state = painter.state) {
            is State.Loading -> if (loading != null) {
                loading(state).also { return }
            }
            is State.Success -> if (success != null) {
                success(state).also { return }
            }
            is State.Error -> if (error != null) {
                error(state).also { return }
            }
        }
    }

    val context = LocalContext.current
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }

    // Draw the image painter.
    Layout(
        content = {},
        modifier = modifier
            .then(semantics)
            .clipToBounds()
            .paint(
                painter = painter,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            ),
        measurePolicy = { _, constraints ->
            if (measureSize != null) measureSize.value = constraints.toSize(context)
            layout(constraints.minWidth, constraints.minHeight) {}
        }
    )
}

private fun ContentScale.toScale(): Scale {
    return when (this) {
        ContentScale.Fit, ContentScale.Inside, ContentScale.None -> Scale.FIT
        else -> Scale.FILL
    }
}

private fun Constraints.toSize(context: Context): Size {
    if (isZero) return OriginalSize

    val hasBoundedWidth = hasBoundedWidth
    val hasBoundedHeight = hasBoundedHeight
    if (!hasBoundedWidth && !hasBoundedHeight) return OriginalSize

    return PixelSize(
        width = if (hasBoundedWidth) maxWidth else context.resources.displayMetrics.widthPixels,
        height = if (hasBoundedHeight) maxHeight else context.resources.displayMetrics.heightPixels
    )
}
