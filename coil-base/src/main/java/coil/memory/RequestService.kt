package coil.memory

import android.content.Context
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
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
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.size.ViewSizeResolver
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.util.getLifecycle
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

    @WorkerThread
    fun options(
        request: Request,
        size: Size,
        scale: Scale,
        isOnline: Boolean
    ): Options {
        // Fall back to ARGB_8888 if the requested bitmap config does not pass the checks.
        val isValidConfig = request.isConfigValidForTransformations() && request.isConfigValidForHardware(size)
        val bitmapConfig = if (isValidConfig) request.bitmapConfig else Bitmap.Config.ARGB_8888

        // Disable fetching from the network if we know it's offline.
        val networkCachePolicy = if (isOnline) request.networkCachePolicy else CachePolicy.DISABLED

        // Disable allowRgb565 if there are transformations.
        val allowRgb565 = request.transformations.isEmpty() && request.allowRgb565

        return Options(
            config = bitmapConfig,
            colorSpace = request.colorSpace,
            scale = scale,
            allowRgb565 = allowRgb565,
            headers = request.headers,
            diskCachePolicy = request.diskCachePolicy,
            networkCachePolicy = networkCachePolicy
        )
    }

    private fun LoadRequest.getLifecycle(): Lifecycle? {
        return when {
            lifecycle != null -> lifecycle
            target is ViewTarget<*> -> target.view.context.getLifecycle()
            else -> context.getLifecycle()
        }
    }

    private fun Request.isConfigValidForTransformations(): Boolean {
        return transformations.isEmpty() || Transformation.VALID_CONFIGS.contains(bitmapConfig)
    }

    private fun Request.isConfigValidForHardware(size: Size): Boolean {
        if (SDK_INT < O) return true
        if (bitmapConfig != Bitmap.Config.HARDWARE) return true
        return allowHardware && hardwareBitmapService.allowHardware(size)
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
