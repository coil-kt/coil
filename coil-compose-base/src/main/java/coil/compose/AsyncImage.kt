package coil.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
 * @param alpha Optional opacity to be applied to the [AsyncImagePainter] when it is rendered onscreen.
 * @param colorFilter Optional [ColorFilter] to apply for the [AsyncImagePainter] when it is rendered
 *  onscreen.
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
    val request = requestOf(model)
    val newRequest = request.newBuilder()
    val sizeResolver: ConstraintsSizeResolver?
    if (request.defined.sizeResolver == null) {
        val context = LocalContext.current
        sizeResolver = remember(context) { ConstraintsSizeResolver(context) }
        newRequest.size(sizeResolver)
    } else {
        sizeResolver = null
    }
    if (request.defined.scale == null) {
        newRequest.scale(contentScale.toScale())
    }
    val painter = rememberAsyncImagePainter(newRequest.build(), imageLoader)

    // Draw the content.
    Layout(
        content = {
            // Allow overriding what's drawn for each image painter state.
            if (loading != null || success != null || error != null) {
                when (val state = painter.state) {
                    is State.Loading -> if (loading != null) loading(state).also { return }
                    is State.Success -> if (success != null) success(state).also { return }
                    is State.Error -> if (error != null) error(state).also { return }
                }
            }

            // Draw the image.
            Layout(
                modifier = modifier
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
        },
        measurePolicy = { _, constraints ->
            sizeResolver?.setConstraints(constraints)
            layout(constraints.minWidth, constraints.minHeight) {}
        }
    )
}

private class ConstraintsSizeResolver(private val context: Context) : SizeResolver {

    private val size = MutableStateFlow<Size?>(null)

    override suspend fun size() = size.filterNotNull().first()

    fun setConstraints(constraints: Constraints) {
        size.value = constraints.toSize(context)
    }
}

private fun ContentScale.toScale() = when (this) {
    ContentScale.Fit, ContentScale.Inside, ContentScale.None -> Scale.FIT
    else -> Scale.FILL
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
