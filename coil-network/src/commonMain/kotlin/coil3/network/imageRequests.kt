package coil3.network

import coil3.Extras
import coil3.getExtra
import coil3.request.ImageRequest
import coil3.request.Options

// region httpMethod

/**
 * Set the HTTP method for any network operations performed by this image request.
 */
fun ImageRequest.Builder.httpMethod(method: String) = apply {
    extras[httpMethodKey] = method
}

val ImageRequest.httpMethod: String
    get() = getExtra(httpMethodKey)

val Options.httpMethod: String
    get() = getExtra(httpMethodKey)

val Extras.Key.Companion.httpMethod: Extras.Key<String>
    get() = httpMethodKey

private val httpMethodKey = Extras.Key(default = "GET")

// endregion
// region httpHeaders

/**
 * Set the HTTP headers for any network operations performed by this image request.
 */
fun ImageRequest.Builder.httpHeaders(headers: NetworkHeaders) = apply {
    extras[httpHeadersKey] = headers
}

val ImageRequest.httpHeaders: NetworkHeaders
    get() = getExtra(httpHeadersKey)

val Options.httpHeaders: NetworkHeaders
    get() = getExtra(httpHeadersKey)

val Extras.Key.Companion.httpHeaders: Extras.Key<NetworkHeaders>
    get() = httpHeadersKey

private val httpHeadersKey = Extras.Key(default = NetworkHeaders.EMPTY)

// endregion
