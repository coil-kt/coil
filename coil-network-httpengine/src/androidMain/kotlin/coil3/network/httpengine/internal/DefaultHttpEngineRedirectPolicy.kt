package coil3.network.httpengine.internal

import coil3.network.NetworkRequest
import coil3.network.httpengine.HttpEngineRedirectPolicy
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale
import okio.IOException

internal class DefaultHttpEngineRedirectPolicy : HttpEngineRedirectPolicy {

    override fun checkRedirect(request: NetworkRequest, fromUrl: String, toUrl: String) {
        val from: URI
        val to: URI
        try {
            from = URI(fromUrl)
            to = from.resolve(URI(toUrl)).normalize()
        } catch (exception: URISyntaxException) {
            throw IOException("HttpEngine returned an invalid redirect URL", exception)
        }

        // HttpEngine cannot strip headers or the upload body when following a redirect.
        val hasSensitiveData = request.body != null ||
            request.headers.asMap().keys.any { it !in SAFE_CROSS_ORIGIN_REDIRECT_HEADERS }
        if (!hasSensitiveData) return

        val origin = from.origin()
        if (origin == null || origin != to.origin()) {
            throw IOException("Refusing cross-origin redirect with sensitive request data")
        }
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
