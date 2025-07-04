package coil3

import kotlin.jvm.JvmOverloads
import okio.Path

/**
 * A uniform resource locator. See [RFC 3986](https://tools.ietf.org/html/rfc3986).
 *
 * @see toUri
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
 * A builder for creating or modifying [Uri] instances.
 * This provides a more convenient way to manipulate URI components.
 */
class UriBuilder internal constructor(
    private var scheme: String?,
    private var authority: String?,
    private var path: String?,
    private var queryParameters: MutableMap<String, String>,
    private var fragment: String?
) {
    fun scheme(scheme: String?): UriBuilder = apply { this.scheme = scheme }
    fun authority(authority: String?): UriBuilder = apply { this.authority = authority }
    fun path(path: String?): UriBuilder = apply { this.path = path }
    fun fragment(fragment: String?): UriBuilder = apply { this.fragment = fragment }

    fun appendQueryParameter(key: String, value: String): UriBuilder = apply {
        queryParameters[key] = value
    }

    fun clearQuery(): UriBuilder = apply {
        queryParameters.clear()
    }

    fun build(): Uri {
        val queryString = if (queryParameters.isNotEmpty()) {
            queryParameters.map { (key, value) ->
                "${key}=${value}"
            }.joinToString("&")
        } else {
            null
        }

        return Uri(
            scheme = scheme,
            authority = authority,
            path = path,
            query = queryString,
            fragment = fragment
        )
    }
}

/**
 * Create a [Uri] from parts without parsing.
 *
 * @see toUri
 */
fun Uri(
    scheme: String? = null,
    authority: String? = null,
    path: String? = null,
    query: String? = null,
    fragment: String? = null,
    separator: String = Path.DIRECTORY_SEPARATOR,
): Uri {
    require(scheme != null || authority != null || path != null || query != null || fragment != null) {
        "At least one of scheme, authority, path, query, or fragment must be non-null."
    }

    return Uri(
        data = buildData(scheme, authority, path, query, fragment),
        separator = separator,
        scheme = scheme,
        authority = authority,
        path = path,
        query = query,
        fragment = fragment,
    )
}

/**
 * Create a new [Uri] by copying the properties of the original [Uri] and
 * optionally modifying some of them.
 */
fun Uri.copy(
    scheme: String? = this.scheme,
    authority: String? = this.authority,
    path: String? = this.path,
    query: String? = this.query,
    fragment: String? = this.fragment,
): Uri {
    return Uri(
        scheme = scheme,
        authority = authority,
        path = path,
        query = query,
        fragment = fragment,
        separator = this.separator,
    )
}

/**
 * Return a [UriBuilder] that is initialized with the components of this [Uri].
 * This allows for convenient modification of the URI, such as adding query parameters.
 *
 * @return a new [UriBuilder] instance initialized with this URI's components.
 */
fun Uri.buildUpon(): UriBuilder {
    val currentQueryParameters = query?.split("&")?.mapNotNull { param ->
        val parts = param.split("=", limit = 2)
        if (parts.isNotEmpty()) {
            parts[0] to (parts.getOrNull(1) ?: "")
        } else {
            null
        }
    }?.toMap(LinkedHashMap()) ?: mutableMapOf()

    return UriBuilder(
        scheme = scheme,
        authority = authority,
        path = path,
        queryParameters = currentQueryParameters,
        fragment = fragment
    )
}

private fun buildData(
    scheme: String?,
    authority: String?,
    path: String?,
    query: String?,
    fragment: String?,
) = buildString {
    if (scheme != null) {
        append(scheme)
        append(':')
    }
    if (authority != null) {
        append("//")
        append(authority)
    }
    if (path != null) {
        append(path)
    }
    if (query != null) {
        append('?')
        append(query)
    }
    if (fragment != null) {
        append('#')
        append(fragment)
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
    var openScheme = true
    var schemeEndIndex = -1
    var authorityStartIndex = -1
    var pathStartIndex = -1
    var queryStartIndex = -1
    var fragmentStartIndex = -1
    var index = 0

    while (index < data.length) {
        when (data[index]) {
            ':' -> {
                if (openScheme &&
                    queryStartIndex == -1 &&
                    fragmentStartIndex == -1
                ) {
                    if (index + 2 < original.length &&
                        original[index + 1] == '/' &&
                        original[index + 2] == '/'
                    ) {
                        // Standard URI with an authority (e.g. "file:///path/image.jpg").
                        openScheme = false
                        schemeEndIndex = index
                        authorityStartIndex = index + 3
                        index += 2
                    } else if (data == original) {
                        // Special URI that has no authority (e.g. "file:/path/image.jpg").
                        schemeEndIndex = index
                        authorityStartIndex = index + 1
                        pathStartIndex = index + 1
                        index += 1
                    }
                }
            }
            '/' -> {
                if (pathStartIndex == -1 &&
                    queryStartIndex == -1 &&
                    fragmentStartIndex == -1
                ) {
                    openScheme = false
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
