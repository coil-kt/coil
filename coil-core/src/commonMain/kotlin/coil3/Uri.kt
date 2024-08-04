package coil3

import kotlin.jvm.JvmOverloads
import okio.Path

/**
 * A uniform resource locator (https://www.w3.org/Addressing/URL/url-spec.html).
 */
class Uri internal constructor(
    private val data: String,
    val separator: String,
    val scheme: String?,
    val authority: String?,
    val path: String?,
    val query: String?,
    val fragment: String?,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Uri && other.data == data
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    override fun toString(): String {
        return data
    }
}

/**
 * Return the separate segments of the [Uri.path].
 */
val Uri.pathSegments: List<String>
    get() {
        val path = path ?: return emptyList()
        val segments = mutableListOf<String>()

        var index = -1
        while (index < path.length) {
            val startIndex = index + 1
            index = path.indexOf('/', startIndex)
            if (index == -1) {
                index = path.length
            }

            val segment = path.substring(startIndex, index)
            if (segment.isNotEmpty()) {
                segments += segment
            }
        }
        return segments
    }

/**
 * Returns the URI's [Uri.path] formatted according to the URI's native [Uri.separator].
 */
val Uri.filePath: String?
    get() {
        val pathSegments = pathSegments
        if (pathSegments.isEmpty()) {
            return null
        } else {
            val prefix = if (path!!.startsWith(separator)) separator else ""
            return pathSegments.joinToString(prefix = prefix, separator = separator)
        }
    }

/**
 * Parse this [String] into a [Uri]. This method will not throw if the URI is malformed.
 *
 * @param separator The path separator used to separate URI path elements. By default, this
 *  will be '/' on UNIX systems and '\' on Windows systems.
 */
@JvmOverloads
fun String.toUri(separator: String = Path.DIRECTORY_SEPARATOR): Uri {
    var data = this
    if (separator != "/") {
        data = data.replace(separator, "/")
    }
    return parseUri(data, this, separator)
}

private fun parseUri(
    data: String,
    original: String,
    separator: String,
): Uri {
    var schemeEndIndex = -1
    var authorityStartIndex = -1
    var pathStartIndex = -1
    var queryStartIndex = -1
    var fragmentStartIndex = -1
    var index = 0

    while (index < data.length) {
        when (data[index]) {
            ':' -> {
                if (authorityStartIndex == -1 &&
                    pathStartIndex == -1 &&
                    queryStartIndex == -1 &&
                    fragmentStartIndex == -1
                ) {
                    if (index + 2 < data.length &&
                        data[index + 1] == '/' &&
                        data[index + 2] == '/'
                    ) {
                        schemeEndIndex = index
                        authorityStartIndex = index + 3
                        index += 2
                    } else if (index + 1 < original.length &&
                        original[index + 1] == '/'
                    ) {
                        schemeEndIndex = index
                        authorityStartIndex = index + 1
                    }
                }
            }
            '/' -> {
                if (pathStartIndex == -1 &&
                    queryStartIndex == -1 &&
                    fragmentStartIndex == -1
                ) {
                    pathStartIndex = if (authorityStartIndex == -1) 0 else index
                }
            }
            '?' -> {
                if (queryStartIndex == -1 &&
                    fragmentStartIndex == -1
                ) {
                    queryStartIndex = index + 1
                }
            }
            '#' -> {
                if (fragmentStartIndex == -1) {
                    fragmentStartIndex = index + 1
                }
            }
        }
        index++
    }

    var scheme: String? = null
    var authority: String? = null
    var path: String? = null
    var query: String? = null
    var fragment: String? = null

    val queryEndIndex = minOf(
        if (fragmentStartIndex == -1) Int.MAX_VALUE else fragmentStartIndex - 1,
        data.length,
    )
    val pathEndIndex = minOf(
        if (queryStartIndex == -1) Int.MAX_VALUE else queryStartIndex - 1,
        queryEndIndex,
    )

    if (authorityStartIndex != -1) {
        scheme = data.substring(0, schemeEndIndex)

        val authorityEndIndex = minOf(
            if (pathStartIndex == -1) Int.MAX_VALUE else pathStartIndex,
            pathEndIndex,
        )
        authority = data.substring(authorityStartIndex, authorityEndIndex)
    }

    if (pathStartIndex != -1) {
        path = data.substring(pathStartIndex, pathEndIndex)
    }
    if (queryStartIndex != -1) {
        query = data.substring(queryStartIndex, queryEndIndex)
    }
    if (fragmentStartIndex != -1) {
        fragment = data.substring(fragmentStartIndex, data.length)
    }

    val maxLength = maxOf(
        0,
        maxOf(
            scheme.length,
            authority.length,
            maxOf(
                path.length,
                query.length,
                fragment.length,
            ),
        ) - 2,
    )
    val bytes = ByteArray(maxLength)
    return Uri(
        data = data,
        separator = separator,
        scheme = scheme?.percentDecode(bytes),
        authority = authority?.percentDecode(bytes),
        path = path?.percentDecode(bytes),
        query = query?.percentDecode(bytes),
        fragment = fragment?.percentDecode(bytes),
    )
}

private fun String.percentDecode(bytes: ByteArray): String {
    var size = 0
    var index = 0
    val length = length
    val searchLength = maxOf(0, length - 2)

    while (true) {
        if (index >= searchLength) {
            if (index == size) {
                // Fast path: the string doesn't have any encoded characters.
                return this
            } else if (index >= length) {
                // Slow path: decode the byte array.
                return bytes.decodeToString(endIndex = size)
            }
        } else if (get(index) == '%') {
            try {
                val hex = substring(index + 1, index + 3)
                bytes[size] = hex.toInt(16).toByte()
                size++
                index += 3
                continue
            } catch (_: NumberFormatException) {}
        }

        bytes[size] = get(index).code.toByte()
        size++
        index++
    }
}

private val String?.length: Int
    get() = this?.length ?: 0
