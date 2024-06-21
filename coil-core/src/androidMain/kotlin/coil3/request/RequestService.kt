package coil3.request

import android.graphics.Bitmap
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.MATRIX
import androidx.lifecycle.Lifecycle
import coil3.BitmapImage
import coil3.Extras
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.size.Dimension
import coil3.size.DisplaySizeResolver
import coil3.size.Precision
import coil3.size.Scale
import coil3.size.Size
import coil3.size.SizeResolver
import coil3.size.ViewSizeResolver
import coil3.target.Target
import coil3.target.ViewTarget
import coil3.util.HardwareBitmapService
import coil3.util.Logger
import coil3.util.SystemCallbacks
import coil3.util.VALID_TRANSFORMATION_CONFIGS
import coil3.util.getLifecycle
import coil3.util.isHardware
import coil3.util.safeConfig
import kotlinx.coroutines.Job

internal actual fun RequestService(
    imageLoader: ImageLoader,
    systemCallbacks: SystemCallbacks,
    logger: Logger?,
): RequestService = AndroidRequestService(imageLoader, systemCallbacks, logger)

/** Handles operations that act on [ImageRequest]s. */
internal class AndroidRequestService(
    private val imageLoader: ImageLoader,
    private val systemCallbacks: SystemCallbacks,
    logger: Logger?,
) : RequestService {
    private val hardwareBitmapService = HardwareBitmapService(logger)

    /**
     * Wrap [request] to automatically dispose and/or restart the [ImageRequest]
     * based on its lifecycle.
     */
    override fun requestDelegate(request: ImageRequest, job: Job): RequestDelegate {
        val lifecycle = request.lifecycle ?: request.findLifecycle()
        val target = request.target
        if (target is ViewTarget<*>) {
            return ViewTargetRequestDelegate(imageLoader, request, target, lifecycle, job)
        } else if (lifecycle != GlobalLifecycle) {
            return LifecycleRequestDelegate(lifecycle, job)
        } else {
            return BaseRequestDelegate(job)
        }
    }

    private fun ImageRequest.findLifecycle(): Lifecycle {
        val target = target
        val context = if (target is ViewTarget<*>) target.view.context else context
        return context.getLifecycle() ?: GlobalLifecycle
    }

    override fun sizeResolver(request: ImageRequest): SizeResolver {
        if (request.defined.sizeResolver != null) {
            return request.defined.sizeResolver
        }

        val target = request.target
        if (target is ViewTarget<*>) {
            // CENTER and MATRIX scale types should be decoded at the image's original size.
            val view = target.view
            if (view is ImageView && view.scaleType.let { it == CENTER || it == MATRIX }) {
                return SizeResolver.ORIGINAL
            } else {
                return ViewSizeResolver(view)
            }
        } else {
            // Fall back to the size of the display.
            return DisplaySizeResolver(request.context)
        }
    }

    override fun options(request: ImageRequest, sizeResolver: SizeResolver, size: Size): Options {
        return Options(
            request.context,
            size,
            request.resolveScale(size),
            request.resolvePrecision(sizeResolver),
            request.diskCacheKey,
            request.fileSystem,
            request.memoryCachePolicy,
            request.diskCachePolicy,
            request.networkCachePolicy,
            request.resolveExtras(size),
        )
    }

    private fun ImageRequest.resolveScale(size: Size): Scale {
        // Use `Scale.FIT` if either dimension is undefined.
        if (size.width == Dimension.Undefined || size.height == Dimension.Undefined) {
            return Scale.FIT
        } else {
            return scale
        }
    }

    private fun ImageRequest.resolvePrecision(sizeResolver: SizeResolver): Precision {
        if (defined.precision != null) {
            return defined.precision
        }

        if (defined.sizeResolver == null && sizeResolver is DisplaySizeResolver) {
            return Precision.INEXACT
        }

        // If both our target and size resolver reference the same ImageView, allow the
        // dimensions to be inexact as the ImageView will scale the output image
        // automatically. Else, require the dimensions to be exact.
        if (target is ViewTarget<*> &&
            sizeResolver is ViewSizeResolver<*> &&
            target.view is ImageView &&
            target.view === sizeResolver.view
        ) {
            return Precision.INEXACT
        }

        return Precision.EXACT
    }

    private fun ImageRequest.resolveExtras(size: Size): Extras {
        var bitmapConfig = bitmapConfig
        var allowRgb565 = allowRgb565

        // Fall back to ARGB_8888 if the requested bitmap config does not pass the checks.
        if (!isBitmapConfigValidMainThread(this, size)) {
            bitmapConfig = Bitmap.Config.ARGB_8888
        }

        // Disable allowRgb565 if there are transformations or the requested config is ALPHA_8.
        // ALPHA_8 is a mask config where each pixel is 1 byte so it wouldn't make sense to use
        // RGB_565 as an optimization in that case.
        allowRgb565 = allowRgb565 &&
            transformations.isEmpty() &&
            bitmapConfig != Bitmap.Config.ALPHA_8

        var builder = Extras.Builder(defaults.extras.asMap() + extras.asMap())
        if (bitmapConfig != this.bitmapConfig) {
            builder = builder.set(Extras.Key.bitmapConfig, bitmapConfig)
        }
        if (allowRgb565 != this.allowRgb565) {
            builder = builder.set(Extras.Key.allowRgb565, allowRgb565)
        }
        return builder.build()
    }

    override fun updateOptionsOnWorkerThread(options: Options): Options {
        var changed = false
        var networkCachePolicy = options.networkCachePolicy
        var extras = options.extras

        if (!isBitmapConfigValidWorkerThread(options)) {
            extras = extras.newBuilder()
                .set(Extras.Key.bitmapConfig, Bitmap.Config.ARGB_8888)
                .build()
            changed = true
        }

        if (options.networkCachePolicy.readEnabled && !systemCallbacks.isOnline) {
            // Disable fetching from the network if we know we're offline.
            networkCachePolicy = CachePolicy.DISABLED
            changed = true
        }

        if (changed) {
            return options.copy(
                networkCachePolicy = networkCachePolicy,
                extras = extras,
            )
        } else {
            return options
        }
    }

    /**
     * Return 'true' if [cacheValue] is a valid (i.e. can be returned to its [Target])
     * config for [request].
     */
    override fun isCacheValueValidForHardware(
        request: ImageRequest,
        cacheValue: MemoryCache.Value,
    ): Boolean {
        val image = cacheValue.image as? BitmapImage ?: return true
        val requestedConfig = image.bitmap.safeConfig
        return isConfigValidForHardware(request, requestedConfig)
    }

    /**
     * Return 'true' if [requestedConfig] is a valid (i.e. can be returned to its [Target])
     * config for [request].
     */
    private fun isConfigValidForHardware(
        request: ImageRequest,
        requestedConfig: Bitmap.Config,
    ): Boolean {
        // Short circuit if the requested bitmap config is software.
        if (!requestedConfig.isHardware) {
            return true
        }

        // Ensure the request allows hardware bitmaps.
        if (!request.allowHardware) {
            return false
        }

        // Prevent hardware bitmaps for non-hardware accelerated targets.
        val target = request.target
        if (target is ViewTarget<*> &&
            target.view.run { isAttachedToWindow && !isHardwareAccelerated }) {
            return false
        }

        return true
    }

    /** Return 'true' if the current bitmap config is valid, else use [Bitmap.Config.ARGB_8888]. */
    fun isBitmapConfigValidMainThread(
        request: ImageRequest,
        size: Size,
    ): Boolean {
        val validForTransformations = request.transformations.isEmpty() ||
            request.bitmapConfig in VALID_TRANSFORMATION_CONFIGS
        val validForHardware = !request.bitmapConfig.isHardware ||
            (isConfigValidForHardware(request, request.bitmapConfig) &&
                hardwareBitmapService.allowHardwareMainThread(size))
        return validForTransformations && validForHardware
    }

    /** Return 'true' if the current bitmap config is valid, else use [Bitmap.Config.ARGB_8888]. */
    fun isBitmapConfigValidWorkerThread(
        options: Options,
    ): Boolean {
        return !options.bitmapConfig.isHardware || hardwareBitmapService.allowHardwareWorkerThread()
    }
}
