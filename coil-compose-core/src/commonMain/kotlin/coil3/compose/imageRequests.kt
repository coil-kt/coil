package coil3.compose

import coil3.Extras
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.getExtra
import coil3.request.ImageRequest
import coil3.request.crossfade

/**
 * If enabled and the image request has no placeholder, the current image displayed by the
 * [AsyncImagePainter]/[AsyncImage]/[SubcomposeAsyncImage] will be used as the placeholder
 * for this image request. If disabled (the default), the image request's placeholder is always
 * used as the placeholder even if it is null.
 *
 * When used with [crossfade] this allows crossfading between subsequent image requests without
 * manually setting the placeholder to the previous image.
 *
 * NOTE: This configuration option only works with Compose targets.
 */
@ExperimentalCoilApi
fun ImageRequest.Builder.useExistingImageAsPlaceholder(enable: Boolean) = apply {
    extras[useExistingImageAsPlaceholderKey] = enable
}

@ExperimentalCoilApi
fun ImageLoader.Builder.useExistingImageAsPlaceholder(enable: Boolean) = apply {
    extras[useExistingImageAsPlaceholderKey] = enable
}

@ExperimentalCoilApi
val ImageRequest.useExistingImageAsPlaceholder: Boolean
    get() = getExtra(useExistingImageAsPlaceholderKey)

@ExperimentalCoilApi
val Extras.Key.Companion.useExistingImageAsPlaceholder: Extras.Key<Boolean>
    get() = useExistingImageAsPlaceholderKey

private val useExistingImageAsPlaceholderKey = Extras.Key(default = false)
