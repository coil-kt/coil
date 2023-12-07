package coil3.request

import coil3.Extras
import coil3.ImageLoader
import coil3.getExtra

/**
 * Enable a crossfade animation when a request completes successfully.
 */
actual fun ImageRequest.Builder.crossfade(enable: Boolean) =
    crossfade(DEFAULT_CROSSFADE_MILLIS)

actual fun ImageRequest.Builder.crossfade(durationMillis: Int) = apply {
    extras[crossfadeMillisKey] = durationMillis
}

actual fun ImageLoader.Builder.crossfade(enable: Boolean) =
    crossfade(DEFAULT_CROSSFADE_MILLIS)

actual fun ImageLoader.Builder.crossfade(durationMillis: Int) = apply {
    extras[crossfadeMillisKey] = durationMillis
}

actual val ImageRequest.crossfadeMillis: Int
    get() = getExtra(crossfadeMillisKey)

private val crossfadeMillisKey = Extras.Key(default = 0)
