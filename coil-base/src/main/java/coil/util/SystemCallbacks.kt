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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Proxies [ComponentCallbacks2] and [NetworkObserver.Listener] calls to a
 * weakly referenced [imageLoader].
 *
 * This prevents the system from having a strong reference to the [imageLoader], which allows
 * it be freed naturally by the garbage collector. If the [imageLoader] is freed, it unregisters
 * its callbacks.
 */
internal class SystemCallbacks(
    imageLoader: RealImageLoader,
    private val context: Context,
    isNetworkObserverEnabled: Boolean
) : ComponentCallbacks2, NetworkObserver.Listener {

    @VisibleForTesting internal val imageLoader = WeakReference(imageLoader)
    private val networkObserver = if (isNetworkObserverEnabled) {
        NetworkObserver(context, this, imageLoader.logger)
    } else {
        EmptyNetworkObserver()
    }

    @Volatile private var _isOnline = networkObserver.isOnline
    private val _isShutdown = AtomicBoolean(false)

    val isOnline get() = _isOnline
    val isShutdown get() = _isShutdown.get()

    init {
        context.registerComponentCallbacks(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        imageLoader.get() ?: shutdown()
    }

    override fun onTrimMemory(level: Int) = withImageLoader { imageLoader ->
        imageLoader.logger?.log(TAG, Log.VERBOSE) { "trimMemory, level=$level" }
        imageLoader.onTrimMemory(level)
    }

    override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

    override fun onConnectivityChange(isOnline: Boolean) = withImageLoader { imageLoader ->
        imageLoader.logger?.log(TAG, Log.INFO) { if (isOnline) ONLINE else OFFLINE }
        _isOnline = isOnline
    }

    fun shutdown() {
        if (_isShutdown.getAndSet(true)) return
        context.unregisterComponentCallbacks(this)
        networkObserver.shutdown()
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
