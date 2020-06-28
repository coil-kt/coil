package coil.memory

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.widget.ImageView
import androidx.annotation.WorkerThread
import coil.DefaultRequestOptions
import coil.decode.Options
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import coil.size.DisplaySizeResolver
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.size.ViewSizeResolver
import coil.target.Target
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.util.Logger
import coil.util.isAttachedToWindowCompat
import coil.util.isHardware
import coil.util.scale

/** Handles operations that act on [ImageRequest]s. */
internal class RequestService(
    private val defaults: DefaultRequestOptions,
    private val logger: Logger?
) {

    private val hardwareBitmapService = HardwareBitmapService()

    fun errorResult(request: ImageRequest, throwable: Throwable): ErrorResult {
        val drawable = if (throwable is NullRequestDataException) {
            request.fallback ?: defaults.fallback
        } else {
            request.error ?: defaults.error
        }
        return ErrorResult(drawable, request, throwable)
    }

    fun sizeResolver(request: ImageRequest): SizeResolver {
        val sizeResolver = request.sizeResolver
        val target = request.target
        return when {
            sizeResolver != null -> sizeResolver
            target is ViewTarget<*> -> ViewSizeResolver(target.view)
            else -> DisplaySizeResolver(request.context)
        }
    }

    fun scale(request: ImageRequest, sizeResolver: SizeResolver): Scale {
        val scale = request.scale
        if (scale != null) {
            return scale
        }

        if (sizeResolver is ViewSizeResolver<*>) {
            val view = sizeResolver.view
            if (view is ImageView) {
                return view.scale
            }
        }

        val target = request.target
        if (target is ViewTarget<*>) {
            val view = target.view
            if (view is ImageView) {
                return view.scale
            }
        }

        return Scale.FILL
    }

    fun allowInexactSize(request: ImageRequest, sizeResolver: SizeResolver): Boolean {
        return when (request.precision ?: defaults.precision) {
            Precision.EXACT -> false
            Precision.INEXACT -> true
            Precision.AUTOMATIC -> {
                // If both our target and size resolver reference the same ImageView, allow the
                // dimensions to be inexact as the ImageView will scale the output image automatically.
                val target = request.target
                if (target is ViewTarget<*> &&
                    target.view is ImageView &&
                    sizeResolver is ViewSizeResolver<*> &&
                    sizeResolver.view === target.view) {
                    return true
                }

                // If we implicitly fall back to a DisplaySizeResolver, allow the dimensions to be inexact.
                if (request.sizeResolver == null && sizeResolver is DisplaySizeResolver) {
                    return true
                }

                // Else, require the dimensions to be exact.
                return false
            }
        }
    }

    @WorkerThread
    fun options(
        request: ImageRequest,
        sizeResolver: SizeResolver,
        size: Size,
        scale: Scale,
        isOnline: Boolean
    ): Options {
        // Fall back to ARGB_8888 if the requested bitmap config does not pass the checks.
        val isValidConfig = isConfigValidForTransformations(request) && isConfigValidForHardwareAllocation(request, size)
        val bitmapConfig = if (isValidConfig) request.bitmapConfig ?: defaults.bitmapConfig else Bitmap.Config.ARGB_8888

        // Disable fetching from the network if we know we're offline.
        val networkCachePolicy = if (isOnline) request.networkCachePolicy else CachePolicy.DISABLED

        // Disable allowRgb565 if there are transformations or the requested config is ALPHA_8.
        // ALPHA_8 is a mask config where each pixel is 1 byte so it wouldn't make sense to use RGB_565 as an optimization in that case.
        val allowRgb565 = (request.allowRgb565 ?: defaults.allowRgb565) && request.transformations.isEmpty() && bitmapConfig != Bitmap.Config.ALPHA_8

        return Options(
            config = bitmapConfig,
            colorSpace = request.colorSpace,
            scale = scale,
            allowInexactSize = allowInexactSize(request, sizeResolver),
            allowRgb565 = allowRgb565,
            headers = request.headers,
            parameters = request.parameters,
            memoryCachePolicy = request.memoryCachePolicy ?: defaults.memoryCachePolicy,
            diskCachePolicy = request.diskCachePolicy ?: defaults.diskCachePolicy,
            networkCachePolicy = networkCachePolicy ?: defaults.networkCachePolicy
        )
    }

    /** Return true if [requestedConfig] is a valid (i.e. can be returned to its [Target]) config for [request]. */
    fun isConfigValidForHardware(request: ImageRequest, requestedConfig: Bitmap.Config): Boolean {
        // Short circuit if the requested bitmap config is software.
        if (!requestedConfig.isHardware) return true

        // Ensure the request allows hardware bitmaps.
        if (!(request.allowHardware ?: defaults.allowHardware)) return false

        // Prevent hardware bitmaps for non-hardware accelerated targets.
        val target = request.target
        if (target is ViewTarget<*> && target.view.run { isAttachedToWindowCompat && !isHardwareAccelerated }) return false

        return true
    }

    /**
     * Return true if [request]'s requested bitmap config is valid (i.e. can be returned to its [Target]).
     *
     * This check is similar to [isConfigValidForHardware] except this method also checks
     * that we are able to allocate a new hardware bitmap.
     */
    @WorkerThread
    private fun isConfigValidForHardwareAllocation(request: ImageRequest, size: Size): Boolean {
        return isConfigValidForHardware(request, request.bitmapConfig ?: defaults.bitmapConfig) &&
            hardwareBitmapService.allowHardware(size, logger)
    }

    /** Return true if [ImageRequest.bitmapConfig] is valid given its [Transformation]s. */
    private fun isConfigValidForTransformations(request: ImageRequest): Boolean {
        return request.transformations.isEmpty() || (request.bitmapConfig ?: defaults.bitmapConfig) in VALID_TRANSFORMATION_CONFIGS
    }

    companion object {
        /** An allowlist of valid bitmap configs for the input and output bitmaps of [Transformation.transform]. */
        @JvmField internal val VALID_TRANSFORMATION_CONFIGS = if (SDK_INT >= 26) {
            arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
        } else {
            arrayOf(Bitmap.Config.ARGB_8888)
        }
    }
}
