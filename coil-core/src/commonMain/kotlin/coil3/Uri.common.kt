package coil3

/**
 * A uniform resource locator.
 */
class Uri internal constructor(
    private val data: String,
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

val Uri.pathSegments: List<String>
    get() {
        val path = path ?: return emptyList()

        val segments = mutableListOf<String>()
        var index = 0
        while (index < path.length) {
            val startIndex = index + 1
            index = path.indexOf('/', startIndex = startIndex)
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

fun String.toUri(): Uri = parseUri(this)

private fun parseUri(rawData: String): Uri {
    val data = rawData.percentDecode()
    var authorityStartIndex = -1
    var pathStartIndex = -1
    var queryStartIndex = -1
    var fragmentStartIndex = -1
    var index = 0

    while (index < data.length) {
        when (data[index]) {
            ':' -> {
                if (pathStartIndex == -1 &&
                    authorityStartIndex == -1 &&
                    index + 2 < data.length &&
                    data[index + 1] == '/' &&
                    data[index + 2] == '/') {
                    authorityStartIndex = index + 3
                    index += 2
                }
            }
            '/' -> {
                if (pathStartIndex == -1) {
                    pathStartIndex = index
                }
            }
            '?' -> {
                if (pathStartIndex != -1 && queryStartIndex == -1) {
                    queryStartIndex = index + 1
                }
            }
            '#' -> {
                if (pathStartIndex != -1 && fragmentStartIndex == -1) {
                    fragmentStartIndex = index + 1
                }
            }
        }
        index++
    }

    // The query must come before the fragment.
    if (fragmentStartIndex != -1 && fragmentStartIndex < queryStartIndex) {
        queryStartIndex = -1
        fragmentStartIndex = -1
    }

    var scheme: String? = null
    var authority: String? = null
    var path: String? = null
    var query: String? = null
    var fragment: String? = null

    if (authorityStartIndex != -1) {
        scheme = data.substring(0, authorityStartIndex - 3)

        if (pathStartIndex != -1) {
            authority = data.substring(authorityStartIndex, pathStartIndex)
        }
    }

    val queryEndIndex = minOf(
        if (fragmentStartIndex == -1) Int.MAX_VALUE else fragmentStartIndex - 1,
        data.length,
    )
    val pathEndIndex = minOf(
        if (queryStartIndex == -1) Int.MAX_VALUE else queryStartIndex - 1,
        queryEndIndex,
    )

    if (pathStartIndex != -1) {
        path = data.substring(pathStartIndex, pathEndIndex)
    }
    if (queryStartIndex != -1) {
        query = data.substring(queryStartIndex, queryEndIndex)
    }
    if (fragmentStartIndex != -1) {
        fragment = data.substring(fragmentStartIndex, data.length)
    }

    return Uri(rawData, scheme, authority, path, query, fragment)
}

private fun String.percentDecode(): String {
    val bytes = ByteArray(length)
    var size = 0
    var index = 0

    while (index < length) {
        if (get(index) == '%' && index + 2 < length) {
            val hex = substring(index + 1, index + 3)
            bytes[size] = hex.toInt(16).toByte()
            index += 3
        } else {
            bytes[size] = get(index).code.toByte()
            index++
        }
        size++
    }

    if (size == length) {
        // Fast path: the string doesn't have any encoded characters.
        return this
    } else {
        // Slow path: decode the byte array.
        return bytes.decodeToString(endIndex = size)
    }
}
