package coil3.request

import coil3.ImageLoader

/**
 * Enable a crossfade animation when a request completes successfully.
 */
fun ImageRequest.Builder.crossfade(enable: Boolean) =
    crossfade(if (enable) DEFAULT_CROSSFADE_MILLIS else 0)

expect fun ImageRequest.Builder.crossfade(durationMillis: Int): ImageRequest.Builder

fun ImageLoader.Builder.crossfade(enable: Boolean) =
    crossfade(if (enable) DEFAULT_CROSSFADE_MILLIS else 0)

expect fun ImageLoader.Builder.crossfade(durationMillis: Int): ImageLoader.Builder

expect val ImageRequest.crossfadeMillis: Int

internal const val DEFAULT_CROSSFADE_MILLIS = 200
