package coil.memory

import android.util.Log
import coil.decode.DecodeUtils
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import coil.request.ImageRequest
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.util.Logger
import coil.util.log
import coil.util.safeConfig
import kotlin.math.abs

/** Handles operations related to the memory cache. */
internal class MemoryCacheService(
    private val requestService: RequestService,
    private val logger: Logger?
) {

    /** Return true if [cacheValue] satisfies the [request]. */
    fun isCachedValueValid(
        cacheKey: Key?,
        cacheValue: Value,
        request: ImageRequest,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale
    ): Boolean {
        // Ensure the size of the cached bitmap is valid for the request.
        if (!isSizeValid(cacheKey, cacheValue, request, sizeResolver, size, scale)) {
            return false
        }

        // Ensure we don't return a hardware bitmap if the request doesn't allow it.
        if (!requestService.isConfigValidForHardware(request, cacheValue.bitmap.safeConfig)) {
            logger?.log(TAG, Log.DEBUG) {
                "${request.data}: Cached bitmap is hardware-backed, which is incompatible with the request."
            }
            return false
        }

        // Else, the cached drawable is valid and we can short circuit the request.
        return true
    }

    /** Return true if [cacheValue]'s size satisfies the [request]. */
    private fun isSizeValid(
        cacheKey: Key?,
        cacheValue: Value,
        request: ImageRequest,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale
    ): Boolean {
        when (size) {
            is OriginalSize -> {
                if (cacheValue.isSampled) {
                    logger?.log(TAG, Log.DEBUG) {
                        "${request.data}: Requested original size, but cached image is sampled."
                    }
                    return false
                }
            }
            is PixelSize -> {
                val cachedWidth: Int
                val cachedHeight: Int
                when (val cachedSize = cacheKey?.size) {
                    is PixelSize -> {
                        cachedWidth = cachedSize.width
                        cachedHeight = cachedSize.height
                    }
                    OriginalSize, null -> {
                        val bitmap = cacheValue.bitmap
                        cachedWidth = bitmap.width
                        cachedHeight = bitmap.height
                    }
                }

                // Short circuit the size check if the size is at most 1 pixel off in either dimension.
                // This accounts for the fact that downsampling can often produce images with one dimension
                // at most one pixel off due to rounding.
                if (abs(cachedWidth - size.width) <= 1 && abs(cachedHeight - size.height) <= 1) {
                    return true
                }

                val multiple = DecodeUtils.computeSizeMultiplier(
                    srcWidth = cachedWidth,
                    srcHeight = cachedHeight,
                    dstWidth = size.width,
                    dstHeight = size.height,
                    scale = scale
                )
                if (multiple != 1.0 && !requestService.allowInexactSize(request, sizeResolver)) {
                    logger?.log(TAG, Log.DEBUG) {
                        "${request.data}: Cached image's request size ($cachedWidth, $cachedHeight) " +
                            "does not exactly match the requested size (${size.width}, ${size.height}, $scale)."
                    }
                    return false
                }
                if (multiple > 1.0 && cacheValue.isSampled) {
                    logger?.log(TAG, Log.DEBUG) {
                        "${request.data}: Cached image's request size ($cachedWidth, $cachedHeight) " +
                            "is smaller than the requested size (${size.width}, ${size.height}, $scale)."
                    }
                    return false
                }
            }
        }

        return true
    }

    companion object {
        private const val TAG = "MemoryCacheService"
    }
}
