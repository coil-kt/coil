package coil.memory

import android.graphics.Bitmap
import android.util.Log
import coil.decode.DecodeUtils
import coil.request.Request
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.util.Logger
import coil.util.log
import coil.util.safeConfig

/** Handles operations related to the [MemoryCache]. */
internal class MemoryCacheService(
    private val requestService: RequestService,
    private val logger: Logger?
) {

    companion object {
        private const val TAG = "MemoryCacheService"
    }

    /** Return true if the [Bitmap] returned from [MemoryCache] satisfies the [Request]. */
    fun isCachedValueValid(
        cacheKey: MemoryCache.Key?,
        cacheValue: MemoryCache.Value,
        request: Request,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale
    ): Boolean {
        // Ensure the size of the cached bitmap is valid for the request.
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
}
