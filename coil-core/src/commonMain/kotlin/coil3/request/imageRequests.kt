package coil3.request

import coil3.BitmapImage
import coil3.Extras
import coil3.ImageLoader
import coil3.decode.Decoder
import coil3.fetch.Fetcher
import coil3.getExtra
import coil3.size.Dimension
import coil3.size.Size
import coil3.transform.Transformation
import coil3.util.toImmutableList

// region crossfade

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

// endregion
// region transformations

/**
 * Set [Transformation]s to be applied to the output image.
 */
fun ImageRequest.Builder.transformations(vararg transformations: Transformation) =
    transformations(transformations.toList())

fun ImageRequest.Builder.transformations(transformations: List<Transformation>) = apply {
    extras[transformationsKey] = transformations.toImmutableList()

    var index = 0
    val memoryCacheKey = transformations.joinToString { "${index++}:${it.cacheKey}" }
    memoryCacheKeyExtra("coil#transformations", memoryCacheKey)
}

val ImageRequest.transformations: List<Transformation>
    get() = getExtra(transformationsKey)

val Options.transformations: List<Transformation>
    get() = getExtra(transformationsKey)

val Extras.Key.Companion.transformations: Extras.Key<List<Transformation>>
    get() = transformationsKey

private val transformationsKey = Extras.Key<List<Transformation>>(default = emptyList())

// endregion
// region maxBitmapSize

/**
 * Set the maximum width and height for a bitmap.
 *
 * This value is cooperative and [Fetcher]s and [Decoder]s should respect the width and height
 * values provided by [maxBitmapSize] and not allocate a bitmap with a width/height larger
 * than those dimensions.
 *
 * To allow a bitmap's size to be unrestricted pass [Dimension.Undefined] for [size]'s width and/or
 * height.
 */
fun ImageRequest.Builder.maxBitmapSize(size: Size) = apply {
    extras[maxBitmapSizeKey] = size
}

fun ImageLoader.Builder.maxBitmapSize(size: Size) = apply {
    extras[maxBitmapSizeKey] = size
}

val ImageRequest.maxBitmapSize: Size
    get() = getExtra(maxBitmapSizeKey)

val Options.maxBitmapSize: Size
    get() = getExtra(maxBitmapSizeKey)

val Extras.Key.Companion.maxBitmapSize: Extras.Key<Size>
    get() = maxBitmapSizeKey

// Use 2^12 as a maximum size as it's supported by all modern devices.
private val maxBitmapSizeKey = Extras.Key(default = Size(4_096, 4_096))

// endregion
// region addLastModifiedToFileCacheKey

/**
 * Enables adding a file's last modified timestamp to the memory cache key when loading an image
 * from a file.
 *
 * This allows subsequent requests that load the same file to miss the memory cache if the
 * file has been updated. However, if the memory cache check occurs on the main thread
 * (see [ImageLoader.Builder.interceptorCoroutineContext]) calling this will cause a strict mode
 * violation.
 */
fun ImageRequest.Builder.addLastModifiedToFileCacheKey(enable: Boolean) = apply {
    extras[addLastModifiedToFileCacheKeyKey] = enable
}

fun ImageLoader.Builder.addLastModifiedToFileCacheKey(enable: Boolean) = apply {
    extras[addLastModifiedToFileCacheKeyKey] = enable
}

val ImageRequest.addLastModifiedToFileCacheKey: Boolean
    get() = getExtra(addLastModifiedToFileCacheKeyKey)

val Options.addLastModifiedToFileCacheKey: Boolean
    get() = getExtra(addLastModifiedToFileCacheKeyKey)

val Extras.Key.Companion.addLastModifiedToFileCacheKey: Extras.Key<Boolean>
    get() = addLastModifiedToFileCacheKeyKey

private val addLastModifiedToFileCacheKeyKey = Extras.Key(default = false)

// endregion
// region allowConversionToBitmap

/**
 * Allow converting the result drawable to a bitmap to apply any [transformations].
 *
 * If false and the result drawable is not a [BitmapImage] any [transformations] will
 * be ignored.
 */
fun ImageRequest.Builder.allowConversionToBitmap(enable: Boolean) = apply {
    extras[allowConversionToBitmapKey] = enable
}

fun ImageLoader.Builder.allowConversionToBitmap(enable: Boolean) = apply {
    extras[allowConversionToBitmapKey] = enable
}

val ImageRequest.allowConversionToBitmap: Boolean
    get() = getExtra(allowConversionToBitmapKey)

val Options.allowConversionToBitmap: Boolean
    get() = getExtra(allowConversionToBitmapKey)

val Extras.Key.Companion.allowConversionToBitmap: Extras.Key<Boolean>
    get() = allowConversionToBitmapKey

private val allowConversionToBitmapKey = Extras.Key(default = true)

// endregion
