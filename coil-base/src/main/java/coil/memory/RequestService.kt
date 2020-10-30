package coil.memory

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.WorkerThread
import coil.decode.Options
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.NullRequestDataException
import coil.size.Size
import coil.target.Target
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.util.Logger
import coil.util.allowInexactSize
import coil.util.isAttachedToWindowCompat
import coil.util.isHardware

/** Handles operations that act on [ImageRequest]s. */
internal class RequestService(private val logger: Logger?) {

    private val hardwareBitmapService = HardwareBitmapService()

    fun errorResult(request: ImageRequest, throwable: Throwable): ErrorResult {
        return ErrorResult(
            drawable = if (throwable is NullRequestDataException) request.fallback else request.error,
            request = request,
            throwable = throwable
        )
    }

    @WorkerThread
    fun options(
        request: ImageRequest,
        size: Size,
        isOnline: Boolean
    ): Options {
        // Fall back to ARGB_8888 if the requested bitmap config does not pass the checks.
        val isValidConfig = isConfigValidForTransformations(request) && isConfigValidForHardwareAllocation(request, size)
        val config = if (isValidConfig) request.bitmapConfig else Bitmap.Config.ARGB_8888

        // Disable fetching from the network if we know we're offline.
        val networkCachePolicy = if (isOnline) request.networkCachePolicy else CachePolicy.DISABLED

        // Disable allowRgb565 if there are transformations or the requested config is ALPHA_8.
        // ALPHA_8 is a mask config where each pixel is 1 byte so it wouldn't make sense to use RGB_565 as an optimization in that case.
        val allowRgb565 = request.allowRgb565 && request.transformations.isEmpty() && config != Bitmap.Config.ALPHA_8

        return Options(
            context = request.context,
            config = config,
            colorSpace = request.colorSpace,
            scale = request.scale,
            allowInexactSize = request.allowInexactSize,
            allowRgb565 = allowRgb565,
            premultipliedAlpha = request.premultipliedAlpha,
            headers = request.headers,
            parameters = request.parameters,
            memoryCachePolicy = request.memoryCachePolicy,
            diskCachePolicy = request.diskCachePolicy,
            networkCachePolicy = networkCachePolicy
        )
    }

    /** Return true if [requestedConfig] is a valid (i.e. can be returned to its [Target]) config for [request]. */
    fun isConfigValidForHardware(request: ImageRequest, requestedConfig: Bitmap.Config): Boolean {
        // Short circuit if the requested bitmap config is software.
        if (!requestedConfig.isHardware) return true

        // Ensure the request allows hardware bitmaps.
        if (!request.allowHardware) return false

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
        return isConfigValidForHardware(request, request.bitmapConfig) &&
            hardwareBitmapService.allowHardware(size, logger)
    }

    /** Return true if [ImageRequest.bitmapConfig] is valid given its [Transformation]s. */
    private fun isConfigValidForTransformations(request: ImageRequest): Boolean {
        return request.transformations.isEmpty() || request.bitmapConfig in VALID_TRANSFORMATION_CONFIGS
    }

    companion object {
        /** An allowlist of valid bitmap configs for the input and output bitmaps of [Transformation.transform]. */
        @JvmField val VALID_TRANSFORMATION_CONFIGS = if (SDK_INT >= 26) {
            arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
        } else {
            arrayOf(Bitmap.Config.ARGB_8888)
        }
    }
}
