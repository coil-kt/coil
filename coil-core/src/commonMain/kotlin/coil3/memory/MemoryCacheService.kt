package coil3.memory

import coil3.BitmapImage
import coil3.EventListener
import coil3.ImageLoader
import coil3.annotation.VisibleForTesting
import coil3.decode.DataSource
import coil3.intercept.EngineInterceptor.ExecuteResult
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.RequestService
import coil3.request.SuccessResult
import coil3.request.maxBitmapSize
import coil3.request.transformations
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import coil3.size.pxOrElse
import coil3.util.Logger
import coil3.util.isPlaceholderCached
import coil3.util.log
import kotlin.math.abs

internal class MemoryCacheService(
    private val imageLoader: ImageLoader,
    private val requestService: RequestService,
    private val logger: Logger?,
) {

    /** Create a [MemoryCache.Key] for this request. */
    fun newCacheKey(
        request: ImageRequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener,
    ): MemoryCache.Key? {
        // Fast path: an explicit memory cache key has been set.
        if (request.memoryCacheKey != null) {
            return MemoryCache.Key(request.memoryCacheKey, request.memoryCacheKeyExtras)
        }

        // Slow path: create a new memory cache key.
        eventListener.keyStart(request, mappedData)
        val key = imageLoader.components.key(mappedData, options)
        eventListener.keyEnd(request, key)
        if (key == null) {
            return null
        }

        // Else, create a memory cache key with all extras.
        val extras = request.memoryCacheKeyExtras.toMutableMap()
        if (request.transformations.isNotEmpty()) {
            extras[EXTRA_SIZE] = options.size.toString()
        }
        return MemoryCache.Key(key, extras)
    }

    /** Get the [MemoryCache.Value] for this request. */
    fun getCacheValue(
        request: ImageRequest,
        cacheKey: MemoryCache.Key,
        size: Size,
        scale: Scale,
    ): MemoryCache.Value? {
        if (!request.memoryCachePolicy.readEnabled) return null
        val cacheValue = imageLoader.memoryCache?.get(cacheKey)
        return cacheValue?.takeIf { isCacheValueValid(request, cacheKey, it, size, scale) }
    }

    /** Return 'true' if [cacheValue] satisfies the [request]. */
    @VisibleForTesting
    internal fun isCacheValueValid(
        request: ImageRequest,
        cacheKey: MemoryCache.Key,
        cacheValue: MemoryCache.Value,
        size: Size,
        scale: Scale,
    ): Boolean {
        // Ensure we don't return a hardware bitmap if the request doesn't allow it.
        if (!requestService.isCacheValueValidForHardware(request, cacheValue)) {
            logger?.log(TAG, Logger.Level.Debug) {
                "${request.data}: Cached bitmap is hardware-backed, " +
                    "which is incompatible with the request."
            }
            return false
        }

        // Ensure the size of the cached bitmap is valid for the request.
        return isCacheValueValidForSize(request, cacheKey, cacheValue, size, scale)
    }

    /** Return 'true' if [cacheValue]'s size satisfies the [request]. */
    private fun isCacheValueValidForSize(
        request: ImageRequest,
        cacheKey: MemoryCache.Key,
        cacheValue: MemoryCache.Value,
        size: Size,
        scale: Scale,
    ): Boolean {
        // Requests with a size in their cache key must match the requested size exactly.
        val cacheKeySize = cacheKey.extras[EXTRA_SIZE]
        if (cacheKeySize != null) {
            if (cacheKeySize == size.toString()) {
                return true
            } else {
                logger?.log(TAG, Logger.Level.Debug) {
                    "${request.data}: Memory cached image's size " +
                        "($cacheKeySize) does not exactly match the target size " +
                        "($size)."
                }
                return false
            }
        }

        // Return early if the image is already decoded at full size.
        if (!cacheValue.isSampled && (size.isOriginal || request.precision == Precision.INEXACT)) {
            return true
        }

        val srcWidth = cacheValue.image.width
        val srcHeight = cacheValue.image.height
        val maxSize = if (cacheValue.image is BitmapImage) {
            request.maxBitmapSize
        } else {
            Size.ORIGINAL
        }
        val dstWidth = minOf(
            size.width.pxOrElse { Int.MAX_VALUE },
            maxSize.width.pxOrElse { Int.MAX_VALUE },
        )
        val dstHeight = minOf(
            size.height.pxOrElse { Int.MAX_VALUE },
            maxSize.height.pxOrElse { Int.MAX_VALUE },
        )

        val multiplier: Double
        val difference: Int
        val widthPercent = dstWidth / srcWidth.toDouble()
        val heightPercent = dstHeight / srcHeight.toDouble()
        when (
            if (dstWidth != Int.MAX_VALUE && dstHeight != Int.MAX_VALUE) {
                scale
            } else {
                Scale.FIT
            }
        ) {
            Scale.FILL -> if (widthPercent > heightPercent) {
                multiplier = widthPercent
                difference = abs(dstWidth - srcWidth)
            } else {
                multiplier = heightPercent
                difference = abs(dstHeight - srcHeight)
            }
            Scale.FIT -> if (widthPercent < heightPercent) {
                multiplier = widthPercent
                difference = abs(dstWidth - srcWidth)
            } else {
                multiplier = heightPercent
                difference = abs(dstHeight - srcHeight)
            }
        }

        // Allow one pixel of tolerance to account for downsampling rounding issues.
        if (difference <= 1) {
            return true
        }

        when (request.precision) {
            Precision.EXACT -> if (multiplier == 1.0) {
                return true
            } else {
                logger?.log(TAG, Logger.Level.Debug) {
                    "${request.data}: Memory cached image's size " +
                        "($srcWidth, $srcHeight) does not exactly match the target size " +
                        "($dstWidth, $dstHeight)."
                }
                return false
            }
            Precision.INEXACT -> if (multiplier <= 1.0) {
                return true
            } else {
                logger?.log(TAG, Logger.Level.Debug) {
                    "${request.data}: Memory cached image's size " +
                        "($srcWidth, $srcHeight) is smaller than the target size " +
                        "($dstWidth, $dstHeight)."
                }
                return false
            }
        }
    }

    /** Write [result] to the memory cache. Return 'true' if it was added to the cache. */
    fun setCacheValue(
        cacheKey: MemoryCache.Key?,
        request: ImageRequest,
        result: ExecuteResult,
    ): Boolean {
        if (cacheKey == null ||
            !request.memoryCachePolicy.writeEnabled ||
            !result.image.shareable
        ) {
            return false
        }
        val memoryCache = imageLoader.memoryCache ?: return false

        // Create and set the memory cache value.
        val extras = mutableMapOf<String, Any>()
        extras[EXTRA_IS_SAMPLED] = result.isSampled
        result.diskCacheKey?.let { extras[EXTRA_DISK_CACHE_KEY] = it }
        memoryCache[cacheKey] = MemoryCache.Value(result.image, extras)

        return true
    }

    /** Create a [SuccessResult] from the given [cacheKey] and [cacheValue]. */
    fun newResult(
        chain: Interceptor.Chain,
        request: ImageRequest,
        cacheKey: MemoryCache.Key,
        cacheValue: MemoryCache.Value,
    ) = SuccessResult(
        image = cacheValue.image,
        request = request,
        dataSource = DataSource.MEMORY_CACHE,
        memoryCacheKey = cacheKey,
        diskCacheKey = cacheValue.diskCacheKey,
        isSampled = cacheValue.isSampled,
        isPlaceholderCached = chain.isPlaceholderCached,
    )

    private val MemoryCache.Value.isSampled: Boolean
        get() = (extras[EXTRA_IS_SAMPLED] as? Boolean) ?: false

    private val MemoryCache.Value.diskCacheKey: String?
        get() = extras[EXTRA_DISK_CACHE_KEY] as? String

    companion object {
        private const val TAG = "MemoryCacheService"
        internal const val EXTRA_SIZE = "coil#size"
        internal const val EXTRA_IS_SAMPLED = "coil#is_sampled"
        internal const val EXTRA_DISK_CACHE_KEY = "coil#disk_cache_key"
    }
}
