package coil3.network.okhttp.internal

import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkRequestBody
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import okhttp3.Call
import okhttp3.Headers
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer
import okio.ByteString

@JvmInline
internal value class CallFactoryNetworkClient(
    private val httpClient: Call.Factory,
) : NetworkClient {
    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ) = httpClient.newCall(request.toRequest()).await().use { response ->
        block(response.toNetworkResponse(request))
    }
}

private suspend fun NetworkRequest.toRequest(): Request {
    val request = Request.Builder()
    request.url(url)
    request.method(method, body?.readByteString()?.toRequestBody())
    request.headers(headers.toHeaders())
    return request.build()
}

private suspend fun NetworkRequestBody.readByteString(): ByteString {
    val buffer = Buffer()
    writeTo(buffer)
    return buffer.readByteString()
}

private fun Response.toNetworkResponse(request: NetworkRequest): NetworkResponse {
    return NetworkResponse(
        request = request,
        code = code,
        requestMillis = sentRequestAtMillis,
        responseMillis = receivedResponseAtMillis,
        headers = headers.toNetworkHeaders(),
        body = body?.source()?.let(::NetworkResponseBody),
        delegate = this,
    )
}

private fun NetworkHeaders.toHeaders(): Headers {
    val headers = Headers.Builder()
    for ((key, values) in asMap()) {
        for (value in values) {
            headers.addUnsafeNonAscii(key, value)
        }
    }
    return headers.build()
}

private fun Headers.toNetworkHeaders(): NetworkHeaders {
    val headers = NetworkHeaders.Builder()
    for ((key, values) in this) {
        headers.add(key, values)
    }
    return headers.build()
}
