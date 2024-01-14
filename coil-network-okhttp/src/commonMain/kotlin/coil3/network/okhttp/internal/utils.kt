package coil3.network.okhttp.internal

import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem
import okio.Path

@JvmInline
internal value class OkHttpNetworkClient(
    private val httpClient: OkHttpClient,
) : NetworkClient {
    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ) = httpClient.newCall(request.toRequest()).await().use { response ->
        block(response.toNetworkResponse(request))
    }
}

private fun NetworkRequest.toRequest(): Request {
    val request = Request.Builder()
    request.url(url)
    request.method(method, body?.readByteArray()?.toRequestBody())
    request.headers(headers.toHeaders())
    return request.build()
}

private fun Response.toNetworkResponse(request: NetworkRequest): NetworkResponse {
    return NetworkResponse(
        delegate = this,
        request = request,
        requestMillis = sentRequestAtMillis,
        responseMillis = receivedResponseAtMillis,
        code = code,
        headers = headers.toNetworkHeaders(),
        body = body?.source()?.let(::OkHttpNetworkResponseBody),
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

@JvmInline
private value class OkHttpNetworkResponseBody(
    private val source: BufferedSource,
) : NetworkResponse.Body {

    override fun exhausted(): Boolean {
        return source.exhausted()
    }

    override suspend fun writeTo(sink: BufferedSink) {
        source.readAll(sink)
    }

    override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
        fileSystem.write(path) {
            source.readAll(this)
        }
    }

    override fun close() {
        source.close()
    }
}
