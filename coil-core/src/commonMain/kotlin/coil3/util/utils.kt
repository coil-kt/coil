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
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.NullRequestDataException
import kotlin.experimental.ExperimentalNativeApi
import kotlin.reflect.KClass
import okio.Closeable

internal expect fun println(level: Logger.Level, tag: String, message: String)

internal val DataSource.emoji: String
    get() = when (this) {
        DataSource.MEMORY_CACHE,
        DataSource.MEMORY -> "🧠"
        DataSource.DISK -> "💾"
        DataSource.NETWORK -> "☁️"
    }

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (e: RuntimeException) {
        throw e
    } catch (_: Exception) {}
}

internal fun AutoCloseable.closeQuietly() {
    try {
        close()
    } catch (e: RuntimeException) {
        throw e
    } catch (_: Exception) {}
}

internal val EMPTY_IMAGE_FACTORY: (ImageRequest) -> Image? = { null }

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

internal const val SCHEME_FILE = "file"

internal fun isFileUri(uri: Uri): Boolean {
    return (uri.scheme == null || uri.scheme == SCHEME_FILE) &&
        uri.path != null && !isAssetUri(uri)
}

internal fun ErrorResult(
    request: ImageRequest,
    throwable: Throwable,
): ErrorResult {
    return ErrorResult(
        image = if (throwable is NullRequestDataException) {
            request.fallback() ?: request.error()
        } else {
            request.error()
        },
        request = request,
        throwable = throwable,
    )
}

internal expect fun isAssetUri(uri: Uri): Boolean

@ExperimentalNativeApi // This must be propagated from the underlying native implementation.
internal expect class WeakReference<T : Any>(referred: T) {
    fun get(): T?
    fun clear()
}
