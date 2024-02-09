package coil.util

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.VisibleForTesting
import coil.RealImageLoader
import coil.network.EmptyNetworkObserver
import coil.network.NetworkObserver
import java.lang.ref.WeakReference

/**
 * Proxies [ComponentCallbacks2] and [NetworkObserver.Listener] calls to a weakly referenced
 * [imageLoader].
 *
 * This prevents the system from having a strong reference to the [imageLoader], which allows
 * it be freed automatically by the garbage collector. If the [imageLoader] is freed, it unregisters
 * its callbacks.
 */
internal class SystemCallbacks(
    imageLoader: RealImageLoader,
) : ComponentCallbacks2, NetworkObserver.Listener {
    @VisibleForTesting val imageLoader = WeakReference(imageLoader)
    private var application: Context? = null
    private var networkObserver: NetworkObserver? = null
    @VisibleForTesting var shutdown = false

    private var _isOnline = true
    val isOnline: Boolean
        @Synchronized get() {
            // Register the network observer lazily.
            registerNetworkObserver()
            return _isOnline
        }

    @Synchronized
    fun registerMemoryPressureCallbacks() = withImageLoader { imageLoader ->
        if (application != null) return@withImageLoader

        val application = imageLoader.context
        this.application = application
        application.registerComponentCallbacks(this)
    }

    @Synchronized
    private fun registerNetworkObserver() = withImageLoader { imageLoader ->
        if (networkObserver != null) return@withImageLoader

        val networkObserver = if (imageLoader.options.networkObserverEnabled) {
            NetworkObserver(imageLoader.context, this, imageLoader.logger)
        } else {
            EmptyNetworkObserver()
        }
        this.networkObserver = networkObserver
        this._isOnline = networkObserver.isOnline
    }

    @Synchronized
    fun shutdown() {
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
        imageLoader.logger?.log(TAG, Log.VERBOSE) { "trimMemory, level=$level" }
        imageLoader.onTrimMemory(level)
    }

    @Synchronized
    override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

    @Synchronized
    override fun onConnectivityChange(isOnline: Boolean) = withImageLoader { imageLoader ->
        imageLoader.logger?.log(TAG, Log.INFO) { if (isOnline) ONLINE else OFFLINE }
        _isOnline = isOnline
    }

    private inline fun withImageLoader(block: (RealImageLoader) -> Unit) {
        imageLoader.get()?.let(block) ?: shutdown()
    }

    companion object {
        private const val TAG = "NetworkObserver"
        private const val ONLINE = "ONLINE"
        private const val OFFLINE = "OFFLINE"
    }
}
