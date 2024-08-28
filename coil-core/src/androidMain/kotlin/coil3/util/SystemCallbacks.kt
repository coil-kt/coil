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

internal actual fun SystemCallbacks(
    imageLoader: RealImageLoader,
): SystemCallbacks = AndroidSystemCallbacks(imageLoader)

/**
 * Proxies [ComponentCallbacks2] calls to a weakly referenced [imageLoader].
 *
 * This prevents the system from having a strong reference to the [imageLoader], which allows
 * it be freed automatically by the garbage collector. If the [imageLoader] is freed, it unregisters
 * its callbacks.
 */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
internal class AndroidSystemCallbacks(
    imageLoader: RealImageLoader,
) : SystemCallbacks, ComponentCallbacks2 {
    @VisibleForTesting val imageLoader = WeakReference(imageLoader)
    private var application: Context? = null
    @VisibleForTesting var shutdown = false

    @Synchronized
    override fun registerMemoryPressureCallbacks() = withImageLoader { imageLoader ->
        if (application != null) return@withImageLoader

        val application = imageLoader.options.application
        this.application = application
        application.registerComponentCallbacks(this)
    }

    @Synchronized
    override fun shutdown() {
        if (shutdown) return
        shutdown = true

        application?.unregisterComponentCallbacks(this)
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
            // The app is in the background.
            imageLoader.memoryCache?.clear()
        } else if (level >= TRIM_MEMORY_RUNNING_LOW) {
            // The app is in the foreground, but is running low on memory.
            imageLoader.memoryCache?.apply { trimToSize(size / 2) }
        }
    }

    @Synchronized
    override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

    private inline fun withImageLoader(block: (RealImageLoader) -> Unit) {
        imageLoader.get()?.let(block) ?: shutdown()
    }

    private companion object {
        private const val TAG = "AndroidSystemCallbacks"
    }
}
