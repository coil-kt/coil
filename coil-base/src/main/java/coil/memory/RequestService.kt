package coil.memory

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.Lifecycle
import coil.decode.Options
import coil.lifecycle.GlobalLifecycle
import coil.lifecycle.LifecycleCoroutineDispatcher
import coil.request.CachePolicy
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.Request
import coil.size.DisplaySizeResolver
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.size.ViewSizeResolver
import coil.target.Target
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.util.getLifecycle
import coil.util.isHardware
import coil.util.scale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Handles operations that act on [Request]s. */
internal class RequestService {

    private val hardwareBitmapService = HardwareBitmapService()

    @MainThread
    fun lifecycleInfo(request: Request): LifecycleInfo {
        return when (request) {
            is GetRequest -> LifecycleInfo.GLOBAL
            is LoadRequest -> {
                // Attempt to find the lifecycle for this request.
                val lifecycle = request.getLifecycle()
                return if (lifecycle != null) {
                    LifecycleInfo(
                        lifecycle = lifecycle,
                        mainDispatcher = LifecycleCoroutineDispatcher.create(Dispatchers.Main.immediate, lifecycle)
                    )
                } else {
                    LifecycleInfo.GLOBAL
                }
            }
        }
    }

    fun sizeResolver(request: Request, context: Context): SizeResolver {
        val sizeResolver = request.sizeResolver
        val target = request.target
        return when {
            sizeResolver != null -> sizeResolver
            target is ViewTarget<*> -> ViewSizeResolver(target.view)
            else -> DisplaySizeResolver(context)
        }
    }

    fun scale(request: Request, sizeResolver: SizeResolver): Scale {
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

    fun allowInexactSize(request: Request): Boolean {
        return when (request.precision) {
            Precision.EXACT -> false
            Precision.INEXACT -> true
            Precision.AUTOMATIC -> {
                // ImageViews will automatically scale the image.
                if ((request.target as? ViewTarget<*>)?.view is ImageView) return true

                // If we fall back to a DisplaySizeResolver, allow the dimensions to be inexact.
                if (request.sizeResolver == null && request.target !is ViewTarget<*>) return true

                // Else, require the dimensions to be exact.
                return false
            }
        }
    }

    @WorkerThread
    fun options(
        request: Request,
        size: Size,
        scale: Scale,
        isOnline: Boolean
    ): Options {
        // Fall back to ARGB_8888 if the requested bitmap config does not pass the checks.
        val isValidConfig = isConfigValidForTransformations(request) && isConfigValidForHardwareAllocation(request, size)
        val bitmapConfig = if (isValidConfig) request.bitmapConfig else Bitmap.Config.ARGB_8888

        // Disable fetching from the network if we know we're offline.
        val networkCachePolicy = if (isOnline) request.networkCachePolicy else CachePolicy.DISABLED

        // Disable allowRgb565 if there are transformations.
        val allowRgb565 = request.allowRgb565 && request.transformations.isEmpty()

        return Options(
            config = bitmapConfig,
            colorSpace = request.colorSpace,
            scale = scale,
            allowInexactSize = allowInexactSize(request),
            allowRgb565 = allowRgb565,
            headers = request.headers,
            parameters = request.parameters,
            diskCachePolicy = request.diskCachePolicy,
            networkCachePolicy = networkCachePolicy
        )
    }

    /** Return true if [requestedConfig] is a valid (i.e. can be returned to its [Target]) config for [request]. */
    fun isConfigValidForHardware(request: Request, requestedConfig: Bitmap.Config): Boolean {
        // Short circuit if the requested bitmap config is software.
        if (!requestedConfig.isHardware) return true

        // Ensure the request allows hardware bitmaps.
        if (!request.allowHardware) return false

        // Prevent hardware bitmaps for non-hardware accelerated targets.
        if (request.target.run { this is ViewTarget<*> && !view.isHardwareAccelerated }) return false

        return true
    }

    /**
     * Return true if [request]'s requested bitmap config is valid (i.e. can be returned to its [Target]).
     *
     * This check is similar to [isConfigValidForHardware] except this method also checks
     * that we are able to allocate a new hardware bitmap.
     */
    @WorkerThread
    private fun isConfigValidForHardwareAllocation(request: Request, size: Size): Boolean {
        return isConfigValidForHardware(request, request.bitmapConfig) && hardwareBitmapService.allowHardware(size)
    }

    /** Return true if [Request.bitmapConfig] is valid given its [Transformation]s. */
    private fun isConfigValidForTransformations(request: Request): Boolean {
        return request.transformations.isEmpty() || Transformation.VALID_CONFIGS.contains(request.bitmapConfig)
    }

    private fun LoadRequest.getLifecycle(): Lifecycle? {
        return when {
            lifecycle != null -> lifecycle
            target is ViewTarget<*> -> target.view.context.getLifecycle()
            else -> context.getLifecycle()
        }
    }

    data class LifecycleInfo(
        val lifecycle: Lifecycle,
        val mainDispatcher: CoroutineDispatcher
    ) {

        companion object {
            val GLOBAL = LifecycleInfo(
                lifecycle = GlobalLifecycle,
                mainDispatcher = Dispatchers.Main.immediate
            )
        }
    }
}
