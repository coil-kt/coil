package coil3.network.internal

import coil3.disk.DiskCache
import coil3.network.NetworkHeaders

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

internal expect fun assertNotOnMainThread()

internal const val MIME_TYPE_TEXT_PLAIN = "text/plain"
internal const val CACHE_CONTROL = "Cache-Control"
internal const val CONTENT_TYPE = "Content-Type"
