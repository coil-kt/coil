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

/**
 * Returns `true` if this request prefers the end painter's intrinsic size when
 * calculating the `CrossfadePainter`'s intrinsic size.
 *
 * When enabled, the end painter's intrinsic size takes precedence.
 */
@ExperimentalCoilApi
fun ImageRequest.Builder.preferEndFirstIntrinsicSize(enable: Boolean) = apply {
    extras[preferEndFirstIntrinsicSizeKey] = enable
}

@ExperimentalCoilApi
fun ImageLoader.Builder.preferEndFirstIntrinsicSize(enable: Boolean) = apply {
    extras[preferEndFirstIntrinsicSizeKey] = enable
}

@ExperimentalCoilApi
val ImageRequest.preferEndFirstIntrinsicSize: Boolean
    get() = getExtra(preferEndFirstIntrinsicSizeKey)

@ExperimentalCoilApi
val Extras.Key.Companion.preferEndFirstIntrinsicSize: Extras.Key<Boolean>
    get() = preferEndFirstIntrinsicSizeKey

private val preferEndFirstIntrinsicSizeKey = Extras.Key(default = false)
