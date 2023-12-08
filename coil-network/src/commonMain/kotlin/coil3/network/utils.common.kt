package coil3.network

import coil3.disk.DiskCache
import io.ktor.http.HeadersBuilder
import io.ktor.util.StringValues
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readFully
import okio.BufferedSink
import okio.Closeable

internal fun HeadersBuilder.append(line: String) = apply {
    val index = line.indexOf(':')
    require(index != -1) { "Unexpected header: $line" }
    append(line.substring(0, index).trim(), line.substring(index + 1))
}

internal fun HeadersBuilder.appendAllIfNameAbsent(stringValues: StringValues) = apply {
    stringValues.entries().forEach { (name, values) ->
        if (!contains(name)) {
            appendAll(name, values)
        }
    }
}

internal fun Closeable.closeQuietly() {
    try {
        close()
    } catch (e: RuntimeException) {
        throw e
    } catch (_: Exception) {}
}

internal fun DiskCache.Editor.abortQuietly() {
    try {
        abort()
    } catch (_: Exception) {}
}

/** Write a [ByteReadChannel] to [sink] using streaming. */
internal suspend fun ByteReadChannel.writeTo(sink: BufferedSink) {
    val buffer = ByteArray(OKIO_BUFFER_SIZE)

    while (!isClosedForRead) {
        val packet = readRemaining(buffer.size.toLong())
        if (packet.isEmpty) break

        val bytesRead = packet.remaining.toInt()
        packet.readFully(buffer, 0, bytesRead)
        sink.write(buffer, 0, bytesRead)
    }

    closedCause?.let { throw it }
}

// Okio uses 8 KB internally.
private const val OKIO_BUFFER_SIZE: Int = 8 * 1024

internal fun String.toNonNegativeInt(defaultValue: Int): Int {
    val value = toLongOrNull() ?: return defaultValue
    return when {
        value > Int.MAX_VALUE -> Int.MAX_VALUE
        value < 0 -> 0
        else -> value.toInt()
    }
}

internal expect fun assertNotOnMainThread()

internal const val MIME_TYPE_TEXT_PLAIN = "text/plain"
internal const val CACHE_CONTROL = "Cache-Control"
internal const val CONTENT_TYPE = "Content-Type"
