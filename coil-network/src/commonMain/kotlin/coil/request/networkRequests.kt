package coil.request

import coil.Extras
import coil.getOrDefault
import io.ktor.http.Headers
import io.ktor.http.HttpMethod

/**
 * Set the [HttpMethod] for any network operations performed by this request.
 */
fun ImageRequest.Builder.httpMethod(method: HttpMethod) = apply {
    extras[httpMethodKey] = method
}

val ImageRequest.httpMethod: HttpMethod
    get() = extras.getOrDefault(httpMethodKey, defaults.extras)

val Options.httpMethod: HttpMethod
    get() = extras.getOrDefault(httpMethodKey)

private val httpMethodKey = Extras.Key(default = HttpMethod.Get)

/**
 * Set the [Headers] for any network operations performed by this request.
 */
fun ImageRequest.Builder.httpHeaders(headers: Headers) = apply {
    extras[httpHeadersKey] = headers
}

val ImageRequest.httpHeaders: Headers
    get() = extras.getOrDefault(httpHeadersKey, defaults.extras)

val Options.httpHeaders: Headers
    get() = extras.getOrDefault(httpHeadersKey)

private val httpHeadersKey = Extras.Key(default = Headers.Empty)
