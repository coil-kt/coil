package coil3.network.httpengine

import coil3.network.NetworkRequest
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/**
 * Allows same-origin redirects and cross-origin redirects with only allowed headers and no body.
 * Malformed redirects are rejected.
 *
 * @param allowedCrossOriginRedirectHeaders Header names allowed on cross-origin redirects, compared
 * case-insensitively. This collection replaces the default allowlist and is copied on construction.
 */
class CrossOriginHttpEngineRedirectPolicy(
    allowedCrossOriginRedirectHeaders: Collection<String> = ALLOWED_CROSS_ORIGIN_REDIRECT_HEADERS,
) : HttpEngineRedirectPolicy {
    private val allowedCrossOriginRedirectHeaders = allowedCrossOriginRedirectHeaders
        .mapTo(mutableSetOf()) { it.lowercase(Locale.ROOT) }

    override fun shouldFollowRedirect(
        request: NetworkRequest,
        fromUrl: String,
        toUrl: String,
    ): Boolean {
        val from: URI
        val to: URI
        try {
            from = URI(fromUrl)
            to = from.resolve(URI(toUrl)).normalize()
        } catch (_: URISyntaxException) {
            return false
        }

        // HttpEngine cannot strip headers or the upload body when following a redirect.
        val requiresSameOrigin = request.body != null ||
            request.headers.asMap().keys.any { it !in allowedCrossOriginRedirectHeaders }
        if (!requiresSameOrigin) return true

        val origin = from.origin()
        return origin != null && origin == to.origin()
    }

    private fun URI.origin(): Origin? {
        val scheme = scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = host?.lowercase(Locale.ROOT) ?: return null
        val port = when {
            port >= 0 -> port
            scheme == "https" -> 443
            else -> 80
        }
        return Origin(scheme, host, port)
    }

    private data class Origin(
        val scheme: String,
        val host: String,
        val port: Int,
    )
}

// Treat unknown headers as potentially sensitive because they may carry credentials.
private val ALLOWED_CROSS_ORIGIN_REDIRECT_HEADERS = setOf(
    "accept",
    "accept-encoding",
    "accept-language",
    "cache-control",
    "pragma",
    "user-agent",
)
