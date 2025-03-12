package coil3.util

import coil3.ComponentRegistry
import coil3.EventListener
import coil3.Image
import coil3.Uri
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.intercept.RealInterceptorChain
import coil3.key.Keyer
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.NullRequestDataException
import coil3.request.Options
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.coroutines.CoroutineDispatcher
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

/** Same as [ComponentRegistry.key], but with extra logging. */
@Suppress("UNCHECKED_CAST")
internal fun ComponentRegistry.key(
    data: Any,
    options: Options,
    logger: Logger?,
    tag: String,
): String? {
    var hasKeyerForType = false
    keyers.forEachIndices { (keyer, type) ->
        if (type.isInstance(data)) {
            hasKeyerForType = true
            (keyer as Keyer<Any>).key(data, options)?.let { return it }
        }
    }
    if (!hasKeyerForType) {
        logger?.log(tag, Logger.Level.Warn) {
            "No keyer is registered for data with type '${data::class.simpleName}'. " +
                "Register Keyer<${data::class.simpleName}> in the component registry to " +
                "cache the output image in the memory cache."
        }
    }
    return null
}

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

internal expect fun Image.prepareToDraw()

@OptIn(ExperimentalStdlibApi::class)
internal val CoroutineContext.dispatcher: CoroutineDispatcher?
    get() = get(CoroutineDispatcher)
