package coil.request

import io.ktor.http.Headers
import io.ktor.http.HttpMethod

/**
 * Set the [Headers] for any network operations performed by this request.
 */
fun ImageRequest.Builder.httpMethod(method: HttpMethod) = apply {
    extra("http_method", method)
}

/**
 * Set the [Headers] for any network operations performed by this request.
 */
fun ImageRequest.Builder.httpHeaders(headers: Headers) = apply {
    extra("http_headers", headers)
}
