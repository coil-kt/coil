package coil.request

import coil.Extras
import coil.getExtra
import io.ktor.http.Headers
import io.ktor.http.HttpMethod

// region httpMethod

/**
 * Set the [HttpMethod] for any network operations performed by this request.
 */
fun ImageRequest.Builder.httpMethod(method: HttpMethod) = apply {
    extras[httpMethodKey] = method
}

val ImageRequest.httpMethod: HttpMethod
    get() = getExtra(httpMethodKey)

val Options.httpMethod: HttpMethod
    get() = getExtra(httpMethodKey)

val Extras.Key.Companion.httpMethod: Extras.Key<HttpMethod>
    get() = httpMethodKey

private val httpMethodKey = Extras.Key(default = HttpMethod.Get)

// endregion
// region httpHeaders

/**
 * Set the [Headers] for any network operations performed by this request.
 */
fun ImageRequest.Builder.httpHeaders(headers: Headers) = apply {
    extras[httpHeadersKey] = headers
}

val ImageRequest.httpHeaders: Headers
    get() = getExtra(httpHeadersKey)

val Options.httpHeaders: Headers
    get() = getExtra(httpHeadersKey)

val Extras.Key.Companion.httpHeaders: Extras.Key<Headers>
    get() = httpHeadersKey

private val httpHeadersKey = Extras.Key(default = Headers.Empty)

// endregion
