package coil.util

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.content.res.Configuration
import androidx.annotation.VisibleForTesting
import coil.RealImageLoader
import coil.network.EmptyNetworkObserver
import coil.network.NetworkObserver
import java.lang.ref.WeakReference
import kotlinx.atomicfu.atomic

internal actual fun SystemCallbacks(
    options: RealImageLoader.Options,
): SystemCallbacks = AndroidSystemCallbacks(options)

/**
 * Proxies [ComponentCallbacks2] and [NetworkObserver.Listener] calls to a weakly referenced
 * [imageLoader].
 *
 * This prevents the system from having a strong reference to the [imageLoader], which allows
 * it be freed automatically by the garbage collector. If the [imageLoader] is freed, it unregisters
 * its callbacks.
 */
internal class AndroidSystemCallbacks(
    options: RealImageLoader.Options,
) : SystemCallbacks, ComponentCallbacks2, NetworkObserver.Listener {

    private val applicationContext = options.applicationContext
    private val networkObserver = if (options.isNetworkObserverEnabled) {
        NetworkObserver(applicationContext, this, options.logger)
    } else {
        EmptyNetworkObserver()
    }
    @VisibleForTesting internal var imageLoader: WeakReference<RealImageLoader>? = null

    private val _isOnline = atomic(networkObserver.isOnline)
    private val _isShutdown = atomic(false)

    val isOnline by _isOnline
    val isShutdown by _isShutdown

    override fun register(imageLoader: RealImageLoader) {
        this.imageLoader = WeakReference(imageLoader)
        applicationContext.registerComponentCallbacks(this)
    }

    override fun shutdown() {
        if (_isShutdown.getAndSet(true)) return
        applicationContext.unregisterComponentCallbacks(this)
        networkObserver.shutdown()
        imageLoader = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) = withImageLoader {
        // Do nothing.
    }

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

    override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

    override fun onConnectivityChange(isOnline: Boolean) = withImageLoader { imageLoader ->
        imageLoader.options.logger?.log(TAG, Logger.Level.Info) {
            if (isOnline) {
                "onConnectivityChange: The device is online."
            } else {
                "onConnectivityChange: The device is offline."
            }
        }
        _isOnline.value = isOnline
    }

    private inline fun withImageLoader(block: (RealImageLoader) -> Unit) {
        imageLoader?.get()?.let(block) ?: shutdown()
    }

    private companion object {
        private const val TAG = "AndroidSystemCallbacks"
    }
}
