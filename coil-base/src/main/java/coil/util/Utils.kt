@file:JvmName("-Utils")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Looper
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_INSIDE
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import coil.ComponentRegistry
import coil.ImageLoader
import coil.base.R
import coil.decode.DataSource
import coil.decode.Decoder
import coil.disk.DiskCache
import coil.fetch.Fetcher
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Parameters
import coil.request.ViewTargetRequestManager
import coil.size.Scale
import coil.transform.Transformation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import okhttp3.Call
import okhttp3.Headers
import java.io.Closeable
import java.io.File
import java.util.Optional
import kotlin.coroutines.CoroutineContext

internal val View.requestManager: ViewTargetRequestManager
    get() {
        var manager = getTag(R.id.coil_request_manager) as? ViewTargetRequestManager
        if (manager == null) {
            manager = synchronized(this) {
                // Check again in case coil_request_manager was just set.
                (getTag(R.id.coil_request_manager) as? ViewTargetRequestManager)
                    ?.let { return@synchronized it }

                ViewTargetRequestManager(this).apply {
                    addOnAttachStateChangeListener(this)
                    setTag(R.id.coil_request_manager, this)
                }
            }
        }
        return manager
    }

internal val DataSource.emoji: String
    get() = when (this) {
        DataSource.MEMORY_CACHE,
        DataSource.MEMORY -> Emoji.BRAIN
        DataSource.DISK -> Emoji.FLOPPY
        DataSource.NETWORK -> Emoji.CLOUD
    }

internal val Drawable.width: Int
    get() = (this as? BitmapDrawable)?.bitmap?.width ?: intrinsicWidth

internal val Drawable.height: Int
    get() = (this as? BitmapDrawable)?.bitmap?.height ?: intrinsicHeight

internal val Drawable.isVector: Boolean
    get() = this is VectorDrawable || this is VectorDrawableCompat

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (exception: RuntimeException) {
        throw exception
    } catch (_: Exception) {}
}

internal val ImageView.scale: Scale
    get() = when (scaleType) {
        FIT_START, FIT_CENTER, FIT_END, CENTER_INSIDE -> Scale.FIT
        else -> Scale.FILL
    }

/**
 * Wrap a [Call.Factory] factory as a [Call.Factory] instance.
 * [initializer] is called only once the first time [Call.Factory.newCall] is called.
 */
internal fun lazyCallFactory(initializer: () -> Call.Factory): Call.Factory {
    val lazy: Lazy<Call.Factory> = lazy(initializer)
    return Call.Factory { lazy.value.newCall(it) } // Intentionally not a method reference.
}

/**
 * Modified from [MimeTypeMap.getFileExtensionFromUrl] to be more permissive
 * with special characters.
 */
internal fun MimeTypeMap.getMimeTypeFromUrl(url: String?): String? {
    if (url.isNullOrBlank()) {
        return null
    }

    val extension = url
        .substringBeforeLast('#') // Strip the fragment.
        .substringBeforeLast('?') // Strip the query.
        .substringAfterLast('/') // Get the last path segment.
        .substringAfterLast('.', missingDelimiterValue = "") // Get the file extension.

    return getMimeTypeFromExtension(extension)
}

internal val Uri.firstPathSegment: String?
    get() = pathSegments.firstOrNull()

internal val Configuration.nightMode: Int
    get() = uiMode and Configuration.UI_MODE_NIGHT_MASK

internal val DEFAULT_REQUEST_OPTIONS = DefaultRequestOptions()

/** Required for compatibility with API 25 and below. */
internal val NULL_COLOR_SPACE: ColorSpace? = null

internal val EMPTY_HEADERS = Headers.Builder().build()

internal fun Headers?.orEmpty() = this ?: EMPTY_HEADERS

internal fun Parameters?.orEmpty() = this ?: Parameters.EMPTY

internal fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

internal inline val Any.identityHashCode: Int
    get() = System.identityHashCode(this)

internal inline val CoroutineContext.job: Job
    get() = get(Job)!!

@OptIn(ExperimentalStdlibApi::class)
internal inline val CoroutineContext.dispatcher: CoroutineDispatcher
    get() = get(CoroutineDispatcher)!!

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Deferred<T>.getCompletedOrNull(): T? {
    return try {
        getCompleted()
    } catch (_: Throwable) {
        null
    }
}

internal inline operator fun MemoryCache.get(key: MemoryCache.Key?) = key?.let(::get)

/** https://github.com/coil-kt/coil/issues/675 */
internal val Context.safeCacheDir: File get() = cacheDir.apply { mkdirs() }

internal inline fun ComponentRegistry.Builder.addFirst(
    pair: Pair<Fetcher.Factory<*>, Class<*>>?
) = apply { if (pair != null) fetcherFactories.add(0, pair) }

internal inline fun ComponentRegistry.Builder.addFirst(
    factory: Decoder.Factory?
) = apply { if (factory != null) decoderFactories.add(0, factory) }

internal fun unsupported(): Nothing = error("Unsupported")

/**
 * An allowlist of valid bitmap configs for the input and output bitmaps of
 * [Transformation.transform].
 */
internal val VALID_TRANSFORMATION_CONFIGS = if (SDK_INT >= 26) {
    arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
} else {
    arrayOf(Bitmap.Config.ARGB_8888)
}

/**
 * Prefer hardware bitmaps on API 26 and above since they are optimized for drawing without
 * transformations.
 */
internal val DEFAULT_BITMAP_CONFIG = if (SDK_INT >= 26) {
    Bitmap.Config.HARDWARE
} else {
    Bitmap.Config.ARGB_8888
}

/** A simple [Optional] replacement. */
internal class Option<T : Any>(@JvmField val value: T?)

/**
 * Holds the singleton instance of the disk cache. We need to have a singleton disk cache instance
 * to support creating multiple [ImageLoader]s without specifying the disk cache directory.
 *
 * @see DiskCache.Builder.directory
 */
private var singletonDiskCache: DiskCache? = null

@Synchronized
internal fun singletonDiskCache(context: Context): DiskCache {
    singletonDiskCache?.let { return it }
    val diskCache = DiskCache.Builder(context)
        .directory(context.safeCacheDir.resolve("coil_image_cache"))
        .build()
    return diskCache.also { singletonDiskCache = it }
}

/** Namespaced private utility methods for Coil. */
internal object Utils {

    private const val STANDARD_MEMORY_MULTIPLIER = 0.2
    private const val LOW_MEMORY_MULTIPLIER = 0.15
    private const val DEFAULT_MEMORY_CLASS_MEGABYTES = 256

    fun calculateMemoryCacheSize(context: Context, percent: Double): Int {
        val memoryClassMegabytes = try {
            val activityManager: ActivityManager = context.requireSystemService()
            val isLargeHeap = (context.applicationInfo.flags and ApplicationInfo.FLAG_LARGE_HEAP) != 0
            if (isLargeHeap) activityManager.largeMemoryClass else activityManager.memoryClass
        } catch (_: Exception) {
            DEFAULT_MEMORY_CLASS_MEGABYTES
        }
        return (percent * memoryClassMegabytes * 1024 * 1024).toInt()
    }

    fun defaultMemoryCacheSizePercent(context: Context): Double {
        return try {
            val activityManager: ActivityManager = context.requireSystemService()
            if (activityManager.isLowRamDevice) LOW_MEMORY_MULTIPLIER else STANDARD_MEMORY_MULTIPLIER
        } catch (_: Exception) {
            STANDARD_MEMORY_MULTIPLIER
        }
    }
}
