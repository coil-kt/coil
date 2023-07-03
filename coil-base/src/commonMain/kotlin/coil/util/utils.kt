@file:JvmName("-utils")

package coil.util

import android.content.ContentResolver.SCHEME_FILE
import android.net.Uri
import android.os.Looper
import coil.ComponentRegistry
import coil.EventListener
import coil.decode.DataSource
import coil.decode.Decoder
import coil.disk.DiskCache
import coil.fetch.Fetcher
import coil.intercept.Interceptor
import coil.intercept.RealInterceptorChain
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.ImageRequest
import coil.request.Parameters
import coil.request.Tags
import coil.size.Dimension
import coil.size.Scale
import coil.size.Size
import coil.size.isOriginal
import coil.size.pxOrElse
import kotlin.jvm.JvmName
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Headers
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Closeable

internal inline fun Logger.log(tag: String, level: Logger.Level, message: () -> String) {
    if (minLevel <= level) {
        log(tag, level, message(), null)
    }
}

internal fun Logger.log(tag: String, throwable: Throwable) {
    if (minLevel <= Logger.Level.Error) {
        log(tag, Logger.Level.Error, null, throwable)
    }
}

internal expect fun println(level: Logger.Level, tag: String, message: String)

internal val DataSource.emoji: String
    get() = when (this) {
        DataSource.MEMORY_CACHE,
        DataSource.MEMORY -> "🧠"
        DataSource.DISK -> "💾"
        DataSource.NETWORK -> "☁️"
    }

internal val String.scheme: String?
    get() {
        val index = indexOf("://")
        return if (index == -1) null else substring(0, index)
    }

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (e: RuntimeException) {
        throw e
    } catch (_: Exception) {}
}

/**
 * Return 'true' if the request does not require the output image's size to match the
 * requested dimensions exactly.
 */
internal expect val ImageRequest.allowInexactSize: Boolean

internal fun Parameters?.orEmpty() = this ?: Parameters.EMPTY

internal fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Deferred<T>.getCompletedOrNull(): T? {
    return try {
        getCompleted()
    } catch (_: Throwable) {
        null
    }
}

internal val DEFAULT_REQUEST_OPTIONS = DefaultRequestOptions()

internal inline operator fun MemoryCache.get(key: MemoryCache.Key?) = key?.let(::get)

internal inline fun ComponentRegistry.Builder.addFirst(
    pair: Pair<Fetcher.Factory<*>, KClass<*>>?
) = apply { if (pair != null) fetcherFactories.add(0, pair) }

internal inline fun ComponentRegistry.Builder.addFirst(
    factory: Decoder.Factory?
) = apply { if (factory != null) decoderFactories.add(0, factory) }

internal fun String.toNonNegativeInt(defaultValue: Int): Int {
    val value = toLongOrNull() ?: return defaultValue
    return when {
        value > Int.MAX_VALUE -> Int.MAX_VALUE
        value < 0 -> 0
        else -> value.toInt()
    }
}

internal fun DiskCache.Editor.abortQuietly() {
    try {
        abort()
    } catch (_: Exception) {}
}

internal const val MIME_TYPE_JPEG = "image/jpeg"
internal const val MIME_TYPE_WEBP = "image/webp"
internal const val MIME_TYPE_HEIC = "image/heic"
internal const val MIME_TYPE_HEIF = "image/heif"

internal val Interceptor.Chain.isPlaceholderCached: Boolean
    get() = this is RealInterceptorChain && isPlaceholderCached

internal val Interceptor.Chain.eventListener: EventListener
    get() = if (this is RealInterceptorChain) eventListener else EventListener.NONE

internal fun Int.isMinOrMax() = this == Int.MIN_VALUE || this == Int.MAX_VALUE

internal inline fun Size.widthPx(scale: Scale, original: () -> Int): Int {
    return if (isOriginal) original() else width.toPx(scale)
}

internal inline fun Size.heightPx(scale: Scale, original: () -> Int): Int {
    return if (isOriginal) original() else height.toPx(scale)
}

internal fun Dimension.toPx(scale: Scale): Int = pxOrElse {
    when (scale) {
        Scale.FILL -> Int.MIN_VALUE
        Scale.FIT -> Int.MAX_VALUE
    }
}

internal const val DEFAULT_CROSSFADE_MILLIS = 100

/** Modified from [Headers.Builder.add] */
internal fun Headers.Builder.addUnsafeNonAscii(line: String) = apply {
    val index = line.indexOf(':')
    require(index != -1) { "Unexpected header: $line" }
    addUnsafeNonAscii(line.substring(0, index).trim(), line.substring(index + 1))
}

internal fun Response.requireBody(): ResponseBody {
    return checkNotNull(body) { "response body == null" }
}

internal fun internalExtraKeyOf(name: String) = "coil_$name"
