package coil3.memory

import coil3.EventListener
import coil3.ImageLoader
import coil3.annotation.VisibleForTesting
import coil3.decode.DataSource
import coil3.decode.DecodeUtils
import coil3.intercept.EngineInterceptor.ExecuteResult
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.RequestService
import coil3.request.SuccessResult
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import coil3.size.pxOrElse
import coil3.util.Logger
import coil3.util.allowInexactSize
import coil3.util.isMinOrMax
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
        val extras = createComplexMemoryCacheKeyExtras(request, options)
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
        return isSizeValid(request, cacheKey, cacheValue, size, scale)
    }

    /** Return 'true' if [cacheValue]'s size satisfies the [request]. */
    private fun isSizeValid(
        request: ImageRequest,
        cacheKey: MemoryCache.Key,
        cacheValue: MemoryCache.Value,
        size: Size,
        scale: Scale,
    ): Boolean {
        // The cached value must not be sampled if the image's original size is requested.
        val isSampled = cacheValue.isSampled
        if (size.isOriginal) {
            if (isSampled) {
                logger?.log(TAG, Logger.Level.Debug) {
                    "${request.data}: Requested original size, but cached image is sampled."
                }
                return false
            } else {
                return true
            }
        }

        // The requested dimensions must match the transformation size exactly if it is present.
        // Unlike standard, requests we can't assume transformed bitmaps for the same image have
        // the same aspect ratio.
        val transformationSize = cacheKey.extras[EXTRA_TRANSFORMATION_SIZE]
        if (transformationSize != null) {
            // 'Size.toString' is safe to use to determine equality.
            return transformationSize == size.toString()
        }

        // Compute the scaling factor between the source dimensions and the requested dimensions.
        val srcWidth = cacheValue.image.width
        val srcHeight = cacheValue.image.height
        val dstWidth = size.width.pxOrElse { Int.MAX_VALUE }
        val dstHeight = size.height.pxOrElse { Int.MAX_VALUE }
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = scale,
        )

        // Short circuit the size check if the size is at most 1 pixel off in either dimension.
        // This accounts for the fact that downsampling can often produce images with dimensions
        // at most one pixel off due to rounding.
        val allowInexactSize = request.allowInexactSize
        if (allowInexactSize) {
            val downsampleMultiplier = multiplier.coerceAtMost(1.0)
            if (abs(dstWidth - (downsampleMultiplier * srcWidth)) <= 1 ||
                abs(dstHeight - (downsampleMultiplier * srcHeight)) <= 1
            ) {
                return true
            }
        } else {
            if ((dstWidth.isMinOrMax() || abs(dstWidth - srcWidth) <= 1) &&
                (dstHeight.isMinOrMax() || abs(dstHeight - srcHeight) <= 1)
            ) {
                return true
            }
        }

        // The cached value must be equal to the requested size if precision == exact.
        if (multiplier != 1.0 && !allowInexactSize) {
            logger?.log(TAG, Logger.Level.Debug) {
                "${request.data}: Cached image's request size " +
                    "($srcWidth, $srcHeight) does not exactly match the requested size " +
                    "(${size.width}, ${size.height}, $scale)."
            }
            return false
        }

        // The cached value must be larger than the requested size if the cached value is sampled.
        if (multiplier > 1.0 && isSampled) {
            logger?.log(TAG, Logger.Level.Debug) {
                "${request.data}: Cached image's request size " +
                    "($srcWidth, $srcHeight) is smaller than the requested size " +
                    "(${size.width}, ${size.height}, $scale)."
            }
            return false
        }

        return true
    }

    /** Write [result] to the memory cache. Return 'true' if it was added to the cache. */
    fun setCacheValue(
        cacheKey: MemoryCache.Key?,
        request: ImageRequest,
        result: ExecuteResult,
    ): Boolean {
        if (!request.memoryCachePolicy.writeEnabled) return false
        if (cacheKey == null) return false
        val memoryCache = imageLoader.memoryCache ?: return false

        // TODO: Support adding shareable image to the memory cache.
        if (!result.image.shareable) return false

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
        internal const val EXTRA_TRANSFORMATION_INDEX = "coil#transformation_"
        internal const val EXTRA_TRANSFORMATION_SIZE = "coil#transformation_size"
        internal const val EXTRA_IS_SAMPLED = "coil#is_sampled"
        internal const val EXTRA_DISK_CACHE_KEY = "coil#disk_cache_key"
    }
}

internal expect fun createComplexMemoryCacheKeyExtras(
    request: ImageRequest,
    options: Options,
): Map<String, String>
