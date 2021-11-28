package coil.memory

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.annotation.VisibleForTesting
import coil.EventListener
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.intercept.EngineInterceptor.ExecuteResult
import coil.intercept.Interceptor
import coil.request.ImageRequest
import coil.request.Options
import coil.request.RequestService
import coil.request.SuccessResult
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.size.isOriginal
import coil.size.pxOrElse
import coil.util.Logger
import coil.util.allowInexactSize
import coil.util.forEachIndexedIndices
import coil.util.isPlaceholderCached
import coil.util.log
import coil.util.pxString
import coil.util.safeConfig
import coil.util.toDrawable
import kotlin.math.abs

internal class MemoryCacheService(
    private val imageLoader: ImageLoader,
    private val requestService: RequestService,
    private val logger: Logger?,
) {

    /** Get the [MemoryCache.Key] for this request. */
    fun getKey(
        request: ImageRequest,
        mappedData: Any,
        options: Options,
        eventListener: EventListener
    ): MemoryCache.Key? {
        // Fast path: an explicit memory cache key has been set.
        request.memoryCacheKey?.let { return it }

        // Slow path: create a new memory cache key.
        eventListener.keyStart(request, mappedData)
        val base = imageLoader.components.key(mappedData, options)
        eventListener.keyEnd(request, base)
        if (base == null) return null

        val extras = mutableMapOf<String, String>()
        if (request.transformations.isNotEmpty()) {
            request.transformations.forEachIndexedIndices { index, transformation ->
                extras["coil#transformation_$index"] = transformation.cacheKey
            }
            extras["coil#request_size"] = options.size.toString()
        }
        extras.putAll(request.parameters.cacheKeys())
        return MemoryCache.Key(base, extras)
    }

    /** Get the [MemoryCache.Value] for this request. */
    fun getValue(
        request: ImageRequest,
        cacheKey: MemoryCache.Key,
        size: Size
    ): MemoryCache.Value? {
        if (!request.memoryCachePolicy.readEnabled) return null
        val candidate = imageLoader.memoryCache?.get(cacheKey)
        return candidate?.takeIf { valueSatisfiesRequest(request, it, size) }
    }

    /** Return 'true' if [cacheValue] satisfies the [request]. */
    @VisibleForTesting
    internal fun valueSatisfiesRequest(
        request: ImageRequest,
        cacheValue: MemoryCache.Value,
        size: Size
    ): Boolean {
        // Ensure we don't return a hardware bitmap if the request doesn't allow it.
        if (!requestService.isConfigValidForHardware(request, cacheValue.bitmap.safeConfig)) {
            logger?.log(TAG, Log.DEBUG) {
                "${request.data}: Cached bitmap is hardware-backed, " +
                    "which is incompatible with the request."
            }
            return false
        }

        // Ensure the size of the cached bitmap is valid for the request.
        return isSizeValid(cacheValue, request, size)
    }

    /** Return 'true' if [cacheValue]'s size satisfies the [request]. */
    private fun isSizeValid(
        cacheValue: MemoryCache.Value,
        request: ImageRequest,
        size: Size
    ): Boolean {
        // The cached value must not be sampled if the image's original size is requested.
        if (size.isOriginal) {
            if (cacheValue.isSampled) {
                logger?.log(TAG, Log.DEBUG) {
                    "${request.data}: Requested original size, but cached image is sampled."
                }
                return false
            } else {
                return true
            }
        }

        val srcWidth = cacheValue.bitmap.width
        val srcHeight = cacheValue.bitmap.height
        val dstWidth = size.width.pxOrElse {
            when {
                !cacheValue.isSampled -> srcWidth
                else -> when (request.scale) {
                    Scale.FIT -> Int.MAX_VALUE
                    Scale.FILL -> Int.MIN_VALUE
                }
            }
        }
        val dstHeight = size.height.pxOrElse {
            when {
                !cacheValue.isSampled -> srcHeight
                else -> when (request.scale) {
                    Scale.FIT -> Int.MAX_VALUE
                    Scale.FILL -> Int.MIN_VALUE
                }
            }
        }
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scale = request.scale
        )

        // Short circuit the size check if the size is at most 1 pixel off in either dimension.
        // This accounts for the fact that downsampling can often produce images with dimensions
        // at most one pixel off due to rounding.
        if (request.allowInexactSize) {
            val downsampleMultiplier = multiplier.coerceAtMost(1.0)
            if ((size.width is Dimension.Original || abs(dstWidth - (downsampleMultiplier * srcWidth)) <= 1) ||
                (size.height is Dimension.Original || abs(dstHeight - (downsampleMultiplier * srcHeight)) <= 1)) {
                return true
            }
        } else {
            if (abs(dstWidth - srcWidth) <= 1 && abs(dstHeight - srcHeight) <= 1) {
                return true
            }
        }

        // The cached value must be equal to the requested size if exact size is requested.
        if (multiplier != 1.0 && !request.allowInexactSize) {
            logger?.log(TAG, Log.DEBUG) {
                "${request.data}: Cached image's request size " +
                    "($srcWidth, $srcHeight) does not exactly match the requested size " +
                    "(${size.width.pxString()}, ${size.height.pxString()}, ${request.scale})."
            }
            return false
        }

        // The cached value must be larger than the requested size if the cached value is sampled.
        if (multiplier >= 1.0 && cacheValue.isSampled) {
            logger?.log(TAG, Log.DEBUG) {
                "${request.data}: Cached image's request size " +
                    "($srcWidth, $srcHeight) is smaller than the requested size " +
                    "(${size.width.pxString()}, ${size.height.pxString()}, ${request.scale})."
            }
            return false
        }

        return true
    }

    /** Write [drawable] to the memory cache. Return 'true' if it was added to the cache. */
    fun setValue(
        cacheKey: MemoryCache.Key?,
        request: ImageRequest,
        result: ExecuteResult,
        size: Size
    ): Boolean {
        if (!request.memoryCachePolicy.writeEnabled) return false
        val memoryCache = imageLoader.memoryCache
        if (memoryCache == null || cacheKey == null) return false
        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return false

        // Create and set the memory cache value.
        val extras = mutableMapOf<String, Any>()
        extras[EXTRA_REQUEST_WIDTH] = size.width
        extras[EXTRA_REQUEST_HEIGHT] = size.height
        extras[EXTRA_IS_SAMPLED] = result.isSampled
        result.diskCacheKey?.let { extras[EXTRA_DISK_CACHE_KEY] = it }
        memoryCache[cacheKey] = MemoryCache.Value(bitmap, extras)
        return true
    }

    /** Create a [SuccessResult] from the given [cacheKey] and [cacheValue]. */
    fun newResult(
        chain: Interceptor.Chain,
        request: ImageRequest,
        cacheKey: MemoryCache.Key,
        cacheValue: MemoryCache.Value
    ) = SuccessResult(
        drawable = cacheValue.bitmap.toDrawable(request.context),
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
        @VisibleForTesting internal const val EXTRA_REQUEST_WIDTH = "coil#request_width"
        @VisibleForTesting internal const val EXTRA_REQUEST_HEIGHT = "coil#request_height"
        @VisibleForTesting internal const val EXTRA_IS_SAMPLED = "coil#is_sampled"
        @VisibleForTesting internal const val EXTRA_DISK_CACHE_KEY = "coil#disk_cache_key"
    }
}
