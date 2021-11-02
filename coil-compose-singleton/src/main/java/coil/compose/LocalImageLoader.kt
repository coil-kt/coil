@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.ImagePainter.ExecuteCallback
import coil.compose.ImagePainter.State
import coil.imageLoader
import coil.request.ImageRequest

@ExperimentalCoilApi
@Composable
fun AsyncImage(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    loading: @Composable ((State.Loading) -> Unit)? = null,
    success: @Composable ((State.Success) -> Unit)? = null,
    error: @Composable ((State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) = AsyncImage(
    data = data,
    contentDescription = contentDescription,
    imageLoader = LocalImageLoader.current,
    modifier = modifier,
    loading = loading,
    success = success,
    error = error,
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
    modifier: Modifier = Modifier,
    loading: @Composable ((State.Loading) -> Unit)? = null,
    success: @Composable ((State.Success) -> Unit)? = null,
    error: @Composable ((State.Error) -> Unit)? = null,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
) = AsyncImage(
    request = request,
    contentDescription = contentDescription,
    imageLoader = LocalImageLoader.current,
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
 * Return an [ImagePainter] that will execute an [ImageRequest] using [LocalImageLoader].
 *
 * @param data The [ImageRequest.data] to load.
 * @param onExecute Called immediately before the [ImagePainter] launches an image request.
 *  Return 'true' to proceed with the request. Return 'false' to skip executing the request.
 * @param builder An optional lambda to configure the request.
 */
@Composable
inline fun rememberImagePainter(
    data: Any?,
    onExecute: ExecuteCallback = ExecuteCallback.Lazy,
    builder: ImageRequest.Builder.() -> Unit = {},
): ImagePainter = rememberImagePainter(data, LocalImageLoader.current, onExecute, builder)

/**
 * Return an [ImagePainter] that will execute the [request] using [LocalImageLoader].
 *
 * @param request The [ImageRequest] to execute.
 * @param onExecute Called immediately before the [ImagePainter] launches an image request.
 *  Return 'true' to proceed with the request. Return 'false' to skip executing the request.
 */
@Composable
inline fun rememberImagePainter(
    request: ImageRequest,
    onExecute: ExecuteCallback = ExecuteCallback.Lazy,
): ImagePainter = rememberImagePainter(request, LocalImageLoader.current, onExecute)

/**
 * A pseudo-[CompositionLocal] that returns the current [ImageLoader] for the composition.
 * If a local [ImageLoader] has not been provided, it returns the singleton [ImageLoader].
 */
val LocalImageLoader = ImageLoaderProvidableCompositionLocal()

/** @see LocalImageLoader */
@JvmInline
value class ImageLoaderProvidableCompositionLocal internal constructor(
    private val delegate: ProvidableCompositionLocal<ImageLoader?> = staticCompositionLocalOf { null }
) {

    val current: ImageLoader
        @Composable get() = delegate.current ?: LocalContext.current.imageLoader

    infix fun provides(value: ImageLoader) = delegate provides value

    infix fun providesDefault(value: ImageLoader) = delegate providesDefault value
}
