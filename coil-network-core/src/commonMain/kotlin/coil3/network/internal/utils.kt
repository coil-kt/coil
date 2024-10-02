package coil3.network.internal

import coil3.disk.DiskCache
import coil3.network.CacheStrategy
import coil3.network.CacheStrategy.ReadResult
import coil3.network.CacheStrategy.WriteResult
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.request.Options
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import okio.Buffer

internal fun NetworkHeaders.Builder.append(line: String) = apply {
    val index = line.indexOf(':')
    require(index != -1) { "Unexpected header: $line" }
    add(line.substring(0, index).trim(), line.substring(index + 1))
}

internal fun DiskCache.Editor.abortQuietly() {
    try {
        abort()
    } catch (_: Exception) {}
}

internal suspend fun NetworkResponseBody.readBuffer(): Buffer = use { body ->
    val buffer = Buffer()
    body.writeTo(buffer)
    return buffer
}

internal expect fun assertNotOnMainThread()

internal const val CACHE_CONTROL = "Cache-Control"
internal const val CONTENT_TYPE = "Content-Type"
internal const val HTTP_METHOD_GET = "GET"
internal const val MIME_TYPE_TEXT_PLAIN = "text/plain"
internal const val HTTP_RESPONSE_OK = 200
internal const val HTTP_RESPONSE_NOT_MODIFIED = 304

internal operator fun NetworkHeaders.plus(other: NetworkHeaders): NetworkHeaders {
    val builder = newBuilder()
    for ((key, values) in other.asMap()) {
        builder[key] = values
    }
    return builder.build()
}

internal fun NetworkResponse.requireBody(): NetworkResponseBody {
    return checkNotNull(body) { "body == null" }
}

internal fun AutoCloseable.closeQuietly() {
    try {
        close()
    } catch (e: RuntimeException) {
        throw e
    } catch (_: Exception) {}
}

internal class DefaultCacheStrategy : CacheStrategy {

    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ): ReadResult {
        // Always return the disk cache response.
        return ReadResult(cacheResponse)
    }

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): WriteResult {
        // Combine the disk response and network response headers and update the metadata.
        // Don't update the response body on disk.
        if (networkResponse.code == HTTP_RESPONSE_NOT_MODIFIED && cacheResponse != null) {
            val combinedHeaders = cacheResponse.headers + networkResponse.headers
            return WriteResult(networkResponse.copy(headers = combinedHeaders, body = null))
        }

        // Write the response metadata and response body to disk.
        return WriteResult(networkResponse)
    }
}
