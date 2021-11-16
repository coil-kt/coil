package coil.compose

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import coil.ImageLoader
import coil.compose.AsyncImagePainter.State
import coil.request.ImageRequest
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

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
 */
@Composable
fun AsyncImage(
    model: Any?,
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
    // Create and execute the image request.
    val request = updateRequest(requestOf(model), contentScale)
    val painter = rememberAsyncImagePainter(request, imageLoader)

    // Draw the content.
    BoxWithConstraints(modifier) {
        // Resolve the size for the image request.
        (request.sizeResolver as? ConstraintsSizeResolver)?.setConstraints(constraints)

        // Skip drawing the image if the current state is overridden.
        var draw = true
        when (val state = painter.state) {
            is State.Loading -> if (loading != null) {
                Column { loading(state) }.also { draw = false }
            }
            is State.Success -> if (success != null) {
                Column { success(state) }.also { draw = false }
            }
            is State.Error -> if (error != null) {
                Column { error(state) }.also { draw = false }
            }
        }

        // Draw the image.
        if (draw) {
            Image(
                painter = painter,
                contentDescription = contentDescription,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter
            )
        }
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

private fun ContentScale.toScale(): Scale = when (this) {
    ContentScale.Fit, ContentScale.Inside, ContentScale.None -> Scale.FIT
    else -> Scale.FILL
}

private class ConstraintsSizeResolver(private val context: Context) : SizeResolver {

    private val constraints = MutableStateFlow<Constraints?>(null)

    override suspend fun size() = constraints.filterNotNull().first().toSize()

    fun setConstraints(constraints: Constraints) {
        this.constraints.value = constraints
    }

    private fun Constraints.toSize(): Size {
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
