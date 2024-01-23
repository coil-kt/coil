package coil3.network

import coil3.Extras
import coil3.getExtra
import coil3.network.internal.HTTP_METHOD_GET
import coil3.request.ImageRequest
import coil3.request.Options

// region httpMethod

/**
 * Set the HTTP request method for any network operations performed by this image request.
 */
fun ImageRequest.Builder.httpMethod(method: String) = apply {
    extras[httpMethodKey] = method.uppercase()
}

val ImageRequest.httpMethod: String
    get() = getExtra(httpMethodKey)

val Options.httpMethod: String
    get() = getExtra(httpMethodKey)

val Extras.Key.Companion.httpMethod: Extras.Key<String>
    get() = httpMethodKey

private val httpMethodKey = Extras.Key(default = HTTP_METHOD_GET)

// endregion
// region httpHeaders

/**
 * Set the HTTP request headers for any network operations performed by this image request.
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
// region httpBody

/**
 * Set the HTTP request body for any network operations performed by this image request.
 */
fun ImageRequest.Builder.httpBody(body: NetworkRequestBody) = apply {
    extras[httpBodyKey] = body
}

val ImageRequest.httpBody: NetworkRequestBody?
    get() = getExtra(httpBodyKey)

val Options.httpBody: NetworkRequestBody?
    get() = getExtra(httpBodyKey)

val Extras.Key.Companion.httpBody: Extras.Key<NetworkRequestBody?>
    get() = httpBodyKey

private val httpBodyKey = Extras.Key<NetworkRequestBody?>(default = null)

// endregion
