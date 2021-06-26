@file:Suppress("unused")

package coil.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Return an [ImagePainter] that will execute an [ImageRequest] using [LocalImageLoader].
 *
 * @param data The [ImageRequest.data] to execute.
 * @param onSizeChange Called if the canvas' size changes.
 *  It can be used to control whether to restart the request when the size changes.
 * @param fadeInMillis The duration of the fade in animation to run when the request
 *  completes successfully. Setting this to 0 disables the animation.
 * @param builder An optional lambda to configure the request.
 */
@Composable
fun rememberImagePainter(
    data: Any?,
    onSizeChange: SizeChangeCallback = { _, _ -> false },
    fadeInMillis: Int = 0,
    builder: ImageRequest.Builder.() -> Unit = {},
): ImagePainter = rememberImagePainter(data, LocalImageLoader.current, onSizeChange, fadeInMillis, builder)

/**
 * Return an [ImagePainter] that will execute the [request] using [LocalImageLoader].
 *
 * @param request The [ImageRequest] to execute.
 * @param onSizeChange Called if the canvas' size changes.
 *  It can be used to control whether to restart the request when the size changes.
 * @param fadeInMillis The duration of the fade in animation to run when the request
 *  completes successfully. Setting this to 0 disables the animation.
 */
@Composable
fun rememberImagePainter(
    request: ImageRequest,
    onSizeChange: SizeChangeCallback = { _, _ -> false },
    fadeInMillis: Int = 0,
): ImagePainter = rememberImagePainter(request, LocalImageLoader.current, onSizeChange, fadeInMillis)

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
