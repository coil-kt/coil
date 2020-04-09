package coil.memory

import android.graphics.Bitmap
import android.util.Log
import coil.DefaultRequestOptions
import coil.decode.DecodeUtils
import coil.request.Request
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.util.Logger
import coil.util.bitmapConfigOrDefault
import coil.util.log
import coil.util.safeConfig
import coil.util.toSoftware

/** Handles operations related to the [MemoryCache]. */
internal class MemoryCacheService(
    private val requestService: RequestService,
    private val defaults: DefaultRequestOptions,
    private val logger: Logger?
) {

    companion object {
        private const val TAG = "MemoryCacheService"
    }

    /** Return true if the [Bitmap] returned from [MemoryCache] satisfies the [Request]. */
    fun isCachedDrawableValid(
        cacheKey: MemoryCache.Key?,
        cacheValue: MemoryCache.Value,
        request: Request,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale
    ): Boolean {
        // Ensure the size is valid for the target.
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

        // Allow returning a cached RGB_565 bitmap if allowRgb565 is enabled.
        if ((request.allowRgb565 ?: defaults.allowRgb565) && cacheValue.bitmap.config == Bitmap.Config.RGB_565) {
            return true
        }

        // Ensure the requested config matches the cached config.
        // Hardware and ARGB_8888 bitmaps are treated as equal for this comparison.
        val cachedConfig = cacheValue.bitmap.config.toSoftware()
        val requestedConfig = request.bitmapConfigOrDefault(defaults).toSoftware()
        if (cachedConfig != requestedConfig) {
            logger?.log(TAG, Log.DEBUG) {
                "${request.data}: Cached bitmap's config ($cachedConfig) does not match the requested config ($requestedConfig)."
            }
            return false
        }

        // Else, the cached drawable is valid and we can short circuit the request.
        return true
    }
}
