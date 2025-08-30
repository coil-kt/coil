package coil3.compose

import coil3.Extras
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.getExtra
import coil3.request.ImageRequest

/**
 * If enabled, crossfade animation will be applied not only between placeholder and the loaded image,
 * but also between consecutive images (i.e., when a new image is requested after a previous one has been successfully loaded).
 * This allows for smooth transitions between images, rather than an abrupt replacement or only placeholder→image crossfade.
 *
 * Note: The [coil3.request.crossfade] option must also be enabled for [useExistingImageAsPlaceholder] to take effect.
 * If [coil3.request.crossfade] is not enabled, this option will have no effect.
 *
 * Also note: If a placeholder is set, the crossfade will always occur between the placeholder and the result,
 * so consecutive image crossfading may not be observable in that scenario.
 *
 * Also note: This option is only supported in Jetpack Compose (AsyncImage, AsyncImagePainter).
 * It does not apply to XML-based views.
 */
fun ImageRequest.Builder.useExistingImageAsPlaceholder(enable: Boolean) = apply {
    extras[useExistingImageAsPlaceholderKey] = enable
}

fun ImageLoader.Builder.useExistingImageAsPlaceholder(enable: Boolean) = apply {
    extras[useExistingImageAsPlaceholderKey] = enable
}

@ExperimentalCoilApi
val ImageRequest.useExistingImageAsPlaceholder: Boolean
    get() = getExtra(useExistingImageAsPlaceholderKey)

val Extras.Key.Companion.useExistingImageAsPlaceholder: Extras.Key<Boolean>
    get() = useExistingImageAsPlaceholderKey

private val useExistingImageAsPlaceholderKey = Extras.Key(default = false)

