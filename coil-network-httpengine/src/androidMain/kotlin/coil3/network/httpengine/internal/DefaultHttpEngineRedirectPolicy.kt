package coil3.network.httpengine.internal

import coil3.network.NetworkRequest
import coil3.network.httpengine.HttpEngineRedirectPolicy
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

internal class DefaultHttpEngineRedirectPolicy : HttpEngineRedirectPolicy {

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
        val hasSensitiveData = request.body != null ||
            request.headers.asMap().keys.any { it !in SAFE_CROSS_ORIGIN_REDIRECT_HEADERS }
        if (!hasSensitiveData) return true

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
private val SAFE_CROSS_ORIGIN_REDIRECT_HEADERS = setOf(
    "accept",
    "accept-encoding",
    "accept-language",
    "cache-control",
    "pragma",
    "user-agent",
)
