package coil3.network.httpengine

import coil3.network.NetworkRequest
import coil3.network.httpengine.internal.DefaultHttpEngineRedirectPolicy

/** Controls which redirects an `HttpEngineNetworkClient` follows. */
fun interface HttpEngineRedirectPolicy {

    /**
     * Decide whether to follow a redirect on the client's executor.
     *
     * @param request The original request, including its headers and body.
     * @param fromUrl The URL that returned this redirect.
     * @param toUrl The redirect destination reported by HttpEngine.
     * @return true to follow the redirect or false to fail the request.
     */
    fun shouldFollowRedirect(request: NetworkRequest, fromUrl: String, toUrl: String): Boolean

    companion object {
        /** Rejects malformed redirects and cross-origin redirects with potentially sensitive data. */
        @JvmField val DEFAULT: HttpEngineRedirectPolicy = DefaultHttpEngineRedirectPolicy()
    }
}
