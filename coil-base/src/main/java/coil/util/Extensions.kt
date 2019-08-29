@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.O
import android.os.StatFs
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_INSIDE
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import androidx.collection.arraySetOf
import androidx.core.graphics.drawable.toDrawable
import coil.base.R
import coil.decode.DataSource
import coil.memory.MemoryCache
import coil.memory.ViewTargetRequestManager
import coil.size.Scale
import coil.target.ViewTarget
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Response
import java.io.Closeable

internal suspend inline fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        val callback = ContinuationCallback(this, continuation)
        enqueue(callback)
        continuation.invokeOnCancellation(callback)
    }
}

internal fun Bitmap.Config?.getBytesPerPixel(): Int {
    return when {
        this == Bitmap.Config.ALPHA_8 -> 1
        this == Bitmap.Config.RGB_565 -> 2
        this == Bitmap.Config.ARGB_4444 -> 2
        SDK_INT >= O && this == Bitmap.Config.RGBA_F16 -> 8
        else -> 4
    }
}

internal inline fun <T> MutableList<T>?.orEmpty(): MutableList<T> = this ?: mutableListOf()

internal inline fun <T> MutableList<T>.removeLast(): T? = if (isNotEmpty()) removeAt(lastIndex) else null

internal inline fun <T> arraySetOf(builder: MutableSet<T>.() -> Unit): Set<T> = arraySetOf<T>().apply(builder)

internal inline fun ActivityManager.isLowRawDeviceCompat(): Boolean {
    return SDK_INT < KITKAT || isLowRamDevice
}

internal inline fun Bitmap.toDrawable(context: Context): BitmapDrawable = toDrawable(context.resources)

/**
 * Returns the in memory size of the given [Bitmap] in bytes.
 */
internal fun Bitmap.getAllocationByteCountCompat(): Int {
    check(!isRecycled) { "Cannot obtain size for recycled Bitmap: $this [$width x $height] + $config" }

    return try {
        if (SDK_INT >= KITKAT) {
            allocationByteCount
        } else {
            rowBytes * height
        }
    } catch (ignored: Exception) {
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

internal fun MemoryCache.getValue(key: String?): MemoryCache.Value? {
    return key?.let { get(it) }
}

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

/** Convert null and [Bitmap.Config.HARDWARE] configs to [Bitmap.Config.ARGB_8888]. */
internal fun Bitmap.Config?.normalize(): Bitmap.Config {
    return if (this == null || (SDK_INT >= O && this == Bitmap.Config.HARDWARE)) {
        Bitmap.Config.ARGB_8888
    } else {
        this
    }
}

internal val ViewTarget<*>.requestManager: ViewTargetRequestManager
    get() {
        var manager = view.getTag(R.id.coil_request_manager) as? ViewTargetRequestManager
        if (manager == null) {
            manager = ViewTargetRequestManager().apply {
                view.addOnAttachStateChangeListener(this)
                view.setTag(R.id.coil_request_manager, this)
            }
        }
        return manager
    }

internal fun ViewTarget<*>.cancel() = requestManager.setRequest(null)

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

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (rethrown: RuntimeException) {
        throw rethrown
    } catch (ignored: Exception) {}
}

internal val ImageView.scale: Scale
    get() = when (scaleType) {
        FIT_START, FIT_CENTER, FIT_END, CENTER_INSIDE -> Scale.FIT
        else -> Scale.FILL
    }

/** Work around for Kotlin not supporting a self type. */
@PublishedApi
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

internal fun MimeTypeMap.getMimeTypeFromUrl(url: String): String? {
    return getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url))
}

internal val Uri.firstPathSegment: String?
    get() = pathSegments.firstOrNull()
