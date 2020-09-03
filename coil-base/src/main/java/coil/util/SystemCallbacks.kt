package coil.util

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.annotation.VisibleForTesting
import coil.ImageLoader
import coil.RealImageLoader
import coil.network.NetworkObserver
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Proxies [ComponentCallbacks2] and [NetworkObserver.Listener] calls to a weakly referenced [imageLoader].
 *
 * This prevents the system from having a strong reference to the [imageLoader], which allows it be freed
 * naturally even if [ImageLoader.shutdown] is not called. If the [imageLoader] is freed, it unregisters
 * its callbacks.
 */
internal class SystemCallbacks(
    imageLoader: RealImageLoader,
    private val context: Context
) : ComponentCallbacks2, NetworkObserver.Listener {

    @VisibleForTesting internal val imageLoader = WeakReference(imageLoader)
    private val networkObserver = NetworkObserver(context, this, imageLoader.logger)

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

    override fun onTrimMemory(level: Int) {
        imageLoader.get()?.onTrimMemory(level) ?: shutdown()
    }

    override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

    override fun onConnectivityChange(isOnline: Boolean) {
        val imageLoader = imageLoader.get()
        if (imageLoader == null) {
            shutdown()
            return
        }

        _isOnline = isOnline
        imageLoader.logger?.log(TAG, Log.INFO) { if (isOnline) ONLINE else OFFLINE }
    }

    fun shutdown() {
        if (_isShutdown.getAndSet(true)) return
        context.unregisterComponentCallbacks(this)
        networkObserver.shutdown()
    }

    companion object {
        private const val TAG = "NetworkObserver"
        private const val ONLINE = "ONLINE"
        private const val OFFLINE = "OFFLINE"
    }
}
