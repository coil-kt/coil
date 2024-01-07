package coil3.network.internal

import coil3.disk.DiskCache
import io.ktor.http.HeadersBuilder
import io.ktor.util.StringValues
import io.ktor.utils.io.ByteReadChannel
import okio.BufferedSink
import okio.FileSystem
import okio.Path

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

internal fun DiskCache.Editor.abortQuietly() {
    try {
        abort()
    } catch (_: Exception) {}
}

/** Write a [ByteReadChannel] to [sink] using streaming. */
internal expect suspend fun ByteReadChannel.writeTo(sink: BufferedSink)

/** Write a [ByteReadChannel] to [path] natively. */
internal expect suspend fun ByteReadChannel.writeTo(fileSystem: FileSystem, path: Path)

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
