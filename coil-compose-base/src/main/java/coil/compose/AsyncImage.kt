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

@ExperimentalCoilApi
@Composable
fun AsyncImage(
    data: Any?,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    loading: @Composable ((State.Loading) -> Unit)? = null,
    error: @Composable ((State.Error) -> Unit)? = null,
    success: @Composable ((State.Success) -> Unit)? = null,
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
    error = error,
    success = success,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
)

@ExperimentalCoilApi
@Composable
fun AsyncImage(
    request: ImageRequest,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    loading: @Composable ((State.Loading) -> Unit)? = null,
    error: @Composable ((State.Error) -> Unit)? = null,
    success: @Composable ((State.Success) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) {
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }

    val requestBuilder = request.newBuilder()
    val size: MutableStateFlow<Size?>?
    if (request.defined.sizeResolver == null) {
        size = MutableStateFlow(null)
        requestBuilder.size { size.filterNotNull().first() }
    } else {
        size = null
    }
    if (request.defined.scale == null) {
        requestBuilder.scale(contentScale.toScale())
    }
    val painter = rememberImagePainter(requestBuilder.build(), imageLoader)
    val context = LocalContext.current

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
            if (size != null) size.value = constraints.toSize(context)
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

    val displayMetrics = context.resources.displayMetrics
    return PixelSize(
        width = if (hasBoundedWidth) maxWidth else displayMetrics.widthPixels,
        height = if (hasBoundedHeight) maxHeight else displayMetrics.heightPixels
    )
}
