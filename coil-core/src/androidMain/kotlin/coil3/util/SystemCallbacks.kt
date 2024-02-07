package coil3.util

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.content.Context
import android.content.res.Configuration
import coil3.RealImageLoader
import coil3.annotation.VisibleForTesting
import coil3.networkObserverEnabled

internal actual fun SystemCallbacks(
    imageLoader: RealImageLoader,
): SystemCallbacks = AndroidSystemCallbacks(imageLoader)

/**
 * Proxies [ComponentCallbacks2] and [NetworkObserver.Listener] calls to a weakly referenced
 * [imageLoader].
 *
 * This prevents the system from having a strong reference to the [imageLoader], which allows
 * it be freed automatically by the garbage collector. If the [imageLoader] is freed, it unregisters
 * its callbacks.
 */
internal class AndroidSystemCallbacks(
    imageLoader: RealImageLoader,
) : SystemCallbacks, ComponentCallbacks2, NetworkObserver.Listener {
    @VisibleForTesting val imageLoader = WeakReference(imageLoader)
    private var application: Context? = null
    private var networkObserver: NetworkObserver? = null
    @VisibleForTesting var shutdown = false

    private var _isOnline = true
    override val isOnline: Boolean
        @Synchronized get() {
            // Register the network observer lazily.
            registerNetworkObserver()
            return _isOnline
        }

    @Synchronized
    override fun registerMemoryPressureCallbacks() = withImageLoader { imageLoader ->
        if (application != null) return@withImageLoader

        val application = imageLoader.options.application
        this.application = application
        application.registerComponentCallbacks(this)
    }

    @Synchronized
    private fun registerNetworkObserver() = withImageLoader { imageLoader ->
        if (networkObserver != null) return@withImageLoader

        val options = imageLoader.options
        val networkObserver = if (options.networkObserverEnabled) {
            NetworkObserver(options.application, this, options.logger)
        } else {
            EmptyNetworkObserver()
        }
        this.networkObserver = networkObserver
        this._isOnline = networkObserver.isOnline
    }

    @Synchronized
    override fun shutdown() {
        if (shutdown) return
        shutdown = true

        application?.unregisterComponentCallbacks(this)
        networkObserver?.shutdown()
        imageLoader.clear()
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
            "onConnectivityChange: The device is ${if (isOnline) "online" else "offline"}."
        }
        _isOnline = isOnline
    }

    private inline fun withImageLoader(block: (RealImageLoader) -> Unit) {
        imageLoader.get()?.let(block) ?: shutdown()
    }

    private companion object {
        private const val TAG = "AndroidSystemCallbacks"
    }
}
