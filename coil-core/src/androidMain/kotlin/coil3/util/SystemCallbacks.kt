package coil3.util

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.content.Context
import android.content.res.Configuration
import coil3.RealImageLoader
import coil3.annotation.VisibleForTesting
import coil3.memoryCacheMaxSizePercentWhileInBackground

internal actual fun SystemCallbacks(
    imageLoader: RealImageLoader,
): SystemCallbacks = AndroidSystemCallbacks(imageLoader)

/**
 * Proxies [ActivityLifecycleCallbacks] and [ComponentCallbacks2] calls to a weakly referenced
 * [imageLoader].
 *
 * This prevents the system from having a strong reference to the [imageLoader], which allows
 * it be freed automatically by the garbage collector. If the [imageLoader] is freed, it unregisters
 * its callbacks.
 */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
internal class AndroidSystemCallbacks(
    strongImageLoaderReference: RealImageLoader,
) : SystemCallbacks {
    @VisibleForTesting val imageLoader = WeakReference(strongImageLoaderReference)
    @VisibleForTesting val activityCallbacks = ActivityCallbacks(strongImageLoaderReference)
    @VisibleForTesting val componentCallbacks = ComponentCallbacks()
    private var application: Context? = null
    @VisibleForTesting var shutdown = false
        private set

    @Synchronized
    override fun registerMemoryPressureCallbacks() = withImageLoader { imageLoader ->
        if (application != null) return@withImageLoader

        val application = imageLoader.options.application
        this.application = application
        application.registerComponentCallbacks(componentCallbacks)
    }

    @Synchronized
    override fun shutdown() {
        if (shutdown) return
        shutdown = true

        application?.let { application ->
            activityCallbacks.unregister(application)
            application.unregisterComponentCallbacks(componentCallbacks)
        }
        imageLoader.clear()
    }

    private inline fun withImageLoader(block: (RealImageLoader) -> Unit) {
        imageLoader.get()?.let(block) ?: shutdown()
    }

    inner class ActivityCallbacks(
        strongImageLoaderReference: RealImageLoader,
    ) : DefaultActivityLifecycleCallbacks {
        private val backgroundMaxSizePercent = strongImageLoaderReference.options
            .memoryCacheMaxSizePercentWhileInBackground

        fun register(context: Context) {
            if (backgroundMaxSizePercent == 1.0) {
                return
            }

            (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)

            // Restrict the image loader's maximum memory cache size.
            withImageLoader { imageLoader ->
                imageLoader.memoryCache?.apply {
                    maxSize = (backgroundMaxSizePercent * initialMaxSize).toLong()

                    imageLoader.options.logger?.log(TAG, Logger.Level.Verbose) {
                        "Restricting $this's max size to $maxSize bytes."
                    }
                }
            }
        }

        fun unregister(context: Context) {
            if (backgroundMaxSizePercent == 1.0) {
                return
            }

            (context.applicationContext as Application).unregisterActivityLifecycleCallbacks(this)

            // Restore the image loader's maximum memory cache size.
            withImageLoader { imageLoader ->
                imageLoader.memoryCache?.apply {
                    maxSize = initialMaxSize

                    imageLoader.options.logger?.log(TAG, Logger.Level.Verbose) {
                        "Restoring $this's max size to $maxSize bytes."
                    }
                }
            }
        }

        override fun onActivityStarted(activity: Activity) = unregister(activity)
    }

    inner class ComponentCallbacks : ComponentCallbacks2 {

        override fun onTrimMemory(level: Int) = synchronized(this@AndroidSystemCallbacks) {
            withImageLoader { imageLoader ->
                imageLoader.options.logger?.log(TAG, Logger.Level.Verbose) {
                    "trimMemory, level=$level"
                }

                when {
                    level >= TRIM_MEMORY_BACKGROUND -> {
                        // The app is in the background and is in the queue to be killed.
                        imageLoader.memoryCache?.clear()
                    }
                    level >= TRIM_MEMORY_UI_HIDDEN -> {
                        // The app is in the background.
                        activityCallbacks.register(imageLoader.options.application)
                    }
                    level >= TRIM_MEMORY_RUNNING_LOW -> {
                        // The app is in the foreground, but is running low on memory.
                        imageLoader.memoryCache?.apply { trimToSize(size / 2) }
                    }
                }
            }
        }

        override fun onLowMemory() = onTrimMemory(TRIM_MEMORY_COMPLETE)

        override fun onConfigurationChanged(
            newConfig: Configuration,
        ) = synchronized(this@AndroidSystemCallbacks) { withImageLoader {} }
    }

    private companion object {
        private const val TAG = "AndroidSystemCallbacks"
    }
}
