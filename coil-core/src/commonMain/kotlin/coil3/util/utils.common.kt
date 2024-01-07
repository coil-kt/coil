package coil3.util

import coil3.ComponentRegistry
import coil3.EventListener
import coil3.Image
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.Decoder
import coil3.fetch.Fetcher
import coil3.intercept.Interceptor
import coil3.intercept.RealInterceptorChain
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import coil3.size.isOriginal
import coil3.size.pxOrElse
import kotlin.reflect.KClass
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

@PublishedApi // Used by extension modules.
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

internal val EMPTY_IMAGE_FACTORY: (ImageRequest) -> Image? = { null }

internal operator fun MemoryCache.get(key: MemoryCache.Key?) = key?.let(::get)

internal fun ComponentRegistry.Builder.addFirst(
    pair: Pair<Fetcher.Factory<*>, KClass<*>>?
) = apply { if (pair != null) lazyFetcherFactories.add(0) { listOf(pair) } }

internal fun ComponentRegistry.Builder.addFirst(
    factory: Decoder.Factory?
) = apply { if (factory != null) lazyDecoderFactories.add(0) { listOf(factory) } }

internal const val MIME_TYPE_JPEG = "image/jpeg"
internal const val MIME_TYPE_WEBP = "image/webp"
internal const val MIME_TYPE_HEIC = "image/heic"
internal const val MIME_TYPE_HEIF = "image/heif"
internal const val MIME_TYPE_XML = "text/xml"

internal val Interceptor.Chain.isPlaceholderCached: Boolean
    get() = this is RealInterceptorChain && isPlaceholderCached

internal val Interceptor.Chain.eventListener: EventListener
    get() = if (this is RealInterceptorChain) eventListener else EventListener.NONE

internal fun Int.isMinOrMax() = this == Int.MIN_VALUE || this == Int.MAX_VALUE

@PublishedApi // Used by extension modules.
internal inline fun Size.widthPx(scale: Scale, original: () -> Int): Int {
    return if (isOriginal) original() else width.toPx(scale)
}

@PublishedApi // Used by extension modules.
internal inline fun Size.heightPx(scale: Scale, original: () -> Int): Int {
    return if (isOriginal) original() else height.toPx(scale)
}

@PublishedApi // Used by extension modules.
internal fun Dimension.toPx(scale: Scale): Int = pxOrElse {
    when (scale) {
        Scale.FILL -> Int.MIN_VALUE
        Scale.FIT -> Int.MAX_VALUE
    }
}

internal const val SCHEME_FILE = "file"

internal fun isFileUri(uri: Uri): Boolean {
    return (uri.scheme == null || uri.scheme == SCHEME_FILE) &&
        uri.path != null &&
        !isAssetUri(uri)
}

internal expect fun isAssetUri(uri: Uri): Boolean
