package coil.request

import coil.Extras
import coil.ImageLoader
import coil.getExtra

/**
 * Enable a crossfade animation when a request completes successfully.
 */
actual fun ImageRequest.Builder.crossfade(enable: Boolean) =
    crossfade(DEFAULT_CROSSFADE_DURATION_MILLIS)

actual fun ImageRequest.Builder.crossfade(durationMillis: Int) = apply {
    extras[crossfadeMillisKey] = durationMillis
}

actual fun ImageLoader.Builder.crossfade(enable: Boolean) =
    crossfade(DEFAULT_CROSSFADE_DURATION_MILLIS)

actual fun ImageLoader.Builder.crossfade(durationMillis: Int) = apply {
    extras[crossfadeMillisKey] = durationMillis
}

actual val ImageRequest.crossfadeMillis: Int
    get() = getExtra(crossfadeMillisKey)

private val crossfadeMillisKey = Extras.Key(default = 0)
