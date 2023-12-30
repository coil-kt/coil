package coil3.util

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.VisibleForTesting
import coil3.RealImageLoader
import coil3.networkObserverEnabled
import java.lang.ref.WeakReference

internal actual fun SystemCallbacks(): SystemCallbacks = AndroidSystemCallbacks()

/**
 * Proxies [ComponentCallbacks2] and [NetworkObserver.Listener] calls to a weakly referenced
 * [imageLoader].
 *
 * This prevents the system from having a strong reference to the [imageLoader], which allows
 * it be freed automatically by the garbage collector. If the [imageLoader] is freed, it unregisters
 * its callbacks.
 */
internal class AndroidSystemCallbacks : SystemCallbacks, ComponentCallbacks2, NetworkObserver.Listener {

    private var application: Context? = null
    private var networkObserver: NetworkObserver? = null
    @VisibleForTesting var imageLoader: WeakReference<RealImageLoader>? = null

    @Volatile override var isOnline = true
    @Volatile override var isShutdown = false

    @Synchronized
    override fun register(imageLoader: RealImageLoader) {
        val application = imageLoader.options.application
        this.application = application
        this.imageLoader = WeakReference(imageLoader)
        application.registerComponentCallbacks(this)

        val networkObserver = if (imageLoader.options.networkObserverEnabled) {
            NetworkObserver(application, this, imageLoader.options.logger)
        } else {
            EmptyNetworkObserver()
        }
        this.networkObserver = networkObserver
        this.isOnline = networkObserver.isOnline
    }

    @Synchronized
    override fun shutdown() {
        if (isShutdown) return
        isShutdown = true

        application?.unregisterComponentCallbacks(this)
        networkObserver?.shutdown()
        imageLoader?.clear()
        imageLoader = null
    }

    @Synchronized
    override fun onConfigurationChanged(newConfig: Configuration) = withImageLoader {}

    @Synchronized
    override fun onTrimMemory(level: Int) = withImageLoader { imageLoader ->
        imageLoader.options.logger?.log(TAG, Logger.Level.Verbose) {
            "trimMemory, level=$level"
        }
        if (level >= TRIM_MEMORY_BACKGROUND) {
            imageLoader.memoryCache?.clear()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            imageLoader.memoryCache?.apply { trimToSize(size / 2) }
        }
    }

    @Synchronized
    override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

    @Synchronized
    override fun onConnectivityChange(isOnline: Boolean) = withImageLoader { imageLoader ->
        imageLoader.options.logger?.log(TAG, Logger.Level.Info) {
            if (isOnline) {
                "onConnectivityChange: The device is online."
            } else {
                "onConnectivityChange: The device is offline."
            }
        }
        this.isOnline = isOnline
    }

    private inline fun withImageLoader(block: (RealImageLoader) -> Unit) {
        imageLoader?.get()?.let(block) ?: shutdown()
    }

    private companion object {
        private const val TAG = "AndroidSystemCallbacks"
    }
}
