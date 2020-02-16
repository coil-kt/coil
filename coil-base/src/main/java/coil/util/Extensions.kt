@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.os.Build.VERSION_CODES.O
import android.os.Looper
import android.os.StatFs
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_INSIDE
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import coil.base.R
import coil.decode.DataSource
import coil.memory.MemoryCache
import coil.memory.ViewTargetRequestManager
import coil.request.LoadRequest
import coil.request.Parameters
import coil.request.Request
import coil.size.Scale
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Headers
import okhttp3.Response
import java.io.Closeable

internal suspend inline fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        val callback = ContinuationCallback(this, continuation)
        enqueue(callback)
        continuation.invokeOnCancellation(callback)
    }
}

@Suppress("DEPRECATION")
internal fun Bitmap.Config?.getBytesPerPixel(): Int {
    return when {
        this == Bitmap.Config.ALPHA_8 -> 1
        this == Bitmap.Config.RGB_565 -> 2
        this == Bitmap.Config.ARGB_4444 -> 2
        SDK_INT >= O && this == Bitmap.Config.RGBA_F16 -> 8
        else -> 4
    }
}

/** Functionally the same as [Iterable.forEach] except it generates an index-based loop that doesn't use an [Iterator]. */
internal inline fun <T> List<T>.forEachIndices(action: (T) -> Unit) {
    for (i in indices) {
        action(get(i))
    }
}

/** Return the first non-null value returned by [transform]. Generate an index-based loop that doesn't use an [Iterator]. */
internal inline fun <R, T> List<R>.firstNotNullIndices(transform: (R) -> T?): T? {
    for (i in indices) {
        transform(get(i))?.let { return it }
    }
    return null
}

/** Functionally the same as [Iterable.find] except it generates an index-based loop that doesn't use an [Iterator]. */
internal inline fun <T> List<T>.findIndices(predicate: (T) -> Boolean): T? {
    for (i in indices) {
        val value = get(i)
        if (predicate(value)) {
            return value
        }
    }
    return null
}

internal inline fun <T> MutableList<T>.removeLast(): T? = if (isNotEmpty()) removeAt(lastIndex) else null

internal inline fun ActivityManager.isLowRamDeviceCompat(): Boolean {
    return SDK_INT < KITKAT || isLowRamDevice
}

internal inline fun Bitmap.toDrawable(context: Context): BitmapDrawable = toDrawable(context.resources)

/** Returns the in memory size of this [Bitmap] in bytes. */
internal fun Bitmap.getAllocationByteCountCompat(): Int {
    check(!isRecycled) { "Cannot obtain size for recycled Bitmap: $this [$width x $height] + $config" }

    return try {
        if (SDK_INT >= KITKAT) {
            allocationByteCount
        } else {
            rowBytes * height
        }
    } catch (_: Exception) {
        Utils.calculateAllocationByteCount(width, height, config)
    }
}

@Suppress("DEPRECATION")
internal inline fun StatFs.getBlockCountCompat(): Long {
    return if (SDK_INT > JELLY_BEAN_MR2) blockCountLong else blockCount.toLong()
}

@Suppress("DEPRECATION")
internal inline fun StatFs.getBlockSizeCompat(): Long {
    return if (SDK_INT > JELLY_BEAN_MR2) blockSizeLong else blockSize.toLong()
}

internal fun MemoryCache.getValue(key: String?): MemoryCache.Value? = key?.let(::get)

internal fun MemoryCache.putValue(key: String?, value: Drawable, isSampled: Boolean) {
    if (key != null) {
        val bitmap = (value as? BitmapDrawable)?.bitmap
        if (bitmap != null) {
            set(key, bitmap, isSampled)
        }
    }
}

internal inline fun <T> takeIf(take: Boolean, factory: () -> T): T? {
    return if (take) factory() else null
}

internal val Bitmap.Config.isHardware: Boolean
    get() = SDK_INT >= O && this == Bitmap.Config.HARDWARE

/** Guard against null bitmap configs. */
internal val Bitmap.safeConfig: Bitmap.Config
    get() = config ?: Bitmap.Config.ARGB_8888

/** Convert null and [Bitmap.Config.HARDWARE] configs to [Bitmap.Config.ARGB_8888]. */
internal fun Bitmap.Config?.normalize(): Bitmap.Config {
    return if (this == null || isHardware) Bitmap.Config.ARGB_8888 else this
}

internal val View.requestManager: ViewTargetRequestManager
    get() {
        var manager = getTag(R.id.coil_request_manager) as? ViewTargetRequestManager
        if (manager == null) {
            manager = ViewTargetRequestManager().apply {
                addOnAttachStateChangeListener(this)
                setTag(R.id.coil_request_manager, this)
            }
        }
        return manager
    }

internal typealias MultiMutableList<R, T> = MutableList<Pair<R, T>>

internal typealias MultiList<R, T> = List<Pair<R, T>>

internal val DataSource.emoji: String
    get() = when (this) {
        DataSource.MEMORY -> Emoji.BRAIN
        DataSource.DISK -> Emoji.FLOPPY
        DataSource.NETWORK -> Emoji.CLOUD
    }

internal val Drawable.width: Int
    get() = (this as? BitmapDrawable)?.bitmap?.width ?: intrinsicWidth

internal val Drawable.height: Int
    get() = (this as? BitmapDrawable)?.bitmap?.width ?: intrinsicWidth

internal val Drawable.isVector: Boolean
    get() = (this is VectorDrawableCompat) || (SDK_INT > LOLLIPOP && this is VectorDrawable)

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (rethrown: RuntimeException) {
        throw rethrown
    } catch (_: Exception) {}
}

internal val ImageView.scale: Scale
    get() = when (scaleType) {
        FIT_START, FIT_CENTER, FIT_END, CENTER_INSIDE -> Scale.FIT
        else -> Scale.FILL
    }

/** Work around for Kotlin not supporting a self type. */
@Suppress("UNCHECKED_CAST")
internal inline fun <T> Any.self(block: T.() -> Unit): T {
    this as T
    block()
    return this
}

/**
 * Wrap a [Call.Factory] factory as a [Call.Factory] instance.
 * [initializer] is called only once the first time [Call.Factory.newCall] is called.
 */
internal fun lazyCallFactory(initializer: () -> Call.Factory): Call.Factory {
    val lazy: Lazy<Call.Factory> = lazy(initializer)
    return Call.Factory { lazy.value.newCall(it) } // Intentionally not a method reference.
}

/** Modified from [MimeTypeMap.getFileExtensionFromUrl] to be more permissive with special characters. */
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

internal fun Resources.getDrawableCompat(@DrawableRes resId: Int, theme: Resources.Theme?): Drawable {
    return checkNotNull(ResourcesCompat.getDrawable(this, resId, theme))
}

internal val Configuration.nightMode: Int
    get() = uiMode and Configuration.UI_MODE_NIGHT_MASK

private val EMPTY_HEADERS = Headers.Builder().build()

internal fun Headers?.orEmpty() = this ?: EMPTY_HEADERS

internal fun Parameters?.orEmpty() = this ?: Parameters.EMPTY

internal fun Request.isDiskOnlyPreload(): Boolean {
    return this is LoadRequest && target == null && !memoryCachePolicy.writeEnabled
}

internal fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

internal inline val Any.identityHashCode: Int
    get() = System.identityHashCode(this)
