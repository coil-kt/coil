package coil3.request

import coil3.ImageLoader

/**
 * Enable a crossfade animation when a request completes successfully.
 */
expect fun ImageRequest.Builder.crossfade(enable: Boolean): ImageRequest.Builder

expect fun ImageRequest.Builder.crossfade(durationMillis: Int): ImageRequest.Builder

expect fun ImageLoader.Builder.crossfade(enable: Boolean): ImageLoader.Builder

expect fun ImageLoader.Builder.crossfade(durationMillis: Int): ImageLoader.Builder

expect val ImageRequest.crossfadeMillis: Int

internal const val DEFAULT_CROSSFADE_MILLIS = 200
