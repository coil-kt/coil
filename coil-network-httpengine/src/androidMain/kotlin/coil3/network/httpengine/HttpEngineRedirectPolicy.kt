package coil3.network.httpengine

import coil3.network.NetworkRequest
import coil3.network.httpengine.internal.DefaultHttpEngineRedirectPolicy
import okio.IOException

/** Controls which redirects an `HttpEngineNetworkClient` follows. */
fun interface HttpEngineRedirectPolicy {

    /**
     * Check a redirect on the client's executor. Return normally to follow it or throw an
     * [IOException] to fail the request.
     *
     * @param request The original request, including its headers and body.
     * @param fromUrl The URL that returned this redirect.
     * @param toUrl The redirect destination reported by HttpEngine.
     */
    @Throws(IOException::class)
    fun checkRedirect(request: NetworkRequest, fromUrl: String, toUrl: String)

    companion object {
        /** Rejects malformed redirects and cross-origin redirects with potentially sensitive data. */
        @JvmField val DEFAULT: HttpEngineRedirectPolicy = DefaultHttpEngineRedirectPolicy()
    }
}
