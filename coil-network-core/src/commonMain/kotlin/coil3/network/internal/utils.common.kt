package coil3.network.internal

import coil3.disk.DiskCache
import coil3.network.NetworkHeaders
import coil3.network.NetworkResponseBody
import okio.Buffer
import okio.use

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
