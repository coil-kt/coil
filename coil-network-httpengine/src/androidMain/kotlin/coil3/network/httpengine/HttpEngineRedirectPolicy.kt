package coil3.network.httpengine

import coil3.network.NetworkRequest

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
        @JvmField val DEFAULT: HttpEngineRedirectPolicy = CrossOriginHttpEngineRedirectPolicy()
    }
}
