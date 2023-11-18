package coil.request

import coil.Extras
import coil.ImageLoader
import coil.getExtra

/**
 * Enable a crossfade animation when a request completes successfully.
 */
actual fun ImageRequest.Builder.crossfade(durationMillis: Int) = apply {
    extras[crossfadeKey] = durationMillis > 0
}

actual fun ImageLoader.Builder.crossfade(durationMillis: Int) = apply {
    extras[crossfadeKey] = durationMillis > 0
}

actual val ImageRequest.crossfade: Boolean
    get() = getExtra(crossfadeKey)

private val crossfadeKey = Extras.Key(default = false)
