package coil3.network.ktor.internal

import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkRequestBody
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.takeFrom
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.cancel
import kotlin.jvm.JvmInline
import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path

@JvmInline
internal value class KtorNetworkClient(
    private val httpClient: HttpClient,
) : NetworkClient {
    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ) = httpClient.prepareRequest(request.toHttpRequestBuilder()).execute { response ->
        block(response.toNetworkResponse(request))
    }
}

private suspend fun NetworkRequest.toHttpRequestBuilder(): HttpRequestBuilder {
    val request = HttpRequestBuilder()
    request.url.takeFrom(url)
    request.method = HttpMethod.parse(method)
    request.headers.takeFrom(headers)
    body?.readByteArray()?.let(request::setBody)
    return request
}

private suspend fun NetworkRequestBody.readByteArray(): ByteArray {
    val buffer = Buffer()
    writeTo(buffer)
    return buffer.readByteArray()
}

private suspend fun HttpResponse.toNetworkResponse(request: NetworkRequest): NetworkResponse {
    return NetworkResponse(
        request = request,
        code = status.value,
        requestMillis = requestTime.timestamp,
        responseMillis = responseTime.timestamp,
        headers = headers.toNetworkHeaders(),
        body = KtorNetworkResponseBody(bodyAsChannel()),
        delegate = this,
    )
}

private fun HeadersBuilder.takeFrom(headers: NetworkHeaders) {
    for ((key, values) in headers.asMap()) {
        appendAll(key, values)
    }
}

private fun Headers.toNetworkHeaders(): NetworkHeaders {
    val headers = NetworkHeaders.Builder()
    for ((key, values) in entries()) {
        headers[key] = values
    }
    return headers.build()
}

@JvmInline
private value class KtorNetworkResponseBody(
    private val channel: ByteReadChannel,
) : NetworkResponseBody {

    override suspend fun writeTo(sink: BufferedSink) {
        channel.writeTo(sink)
    }

    override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
        channel.writeTo(fileSystem, path)
    }

    override fun close() {
        channel.cancel()
    }
}

/** Write a [ByteReadChannel] to [sink] using streaming. */
internal expect suspend fun ByteReadChannel.writeTo(sink: BufferedSink)

/** Write a [ByteReadChannel] to [path] natively. */
internal expect suspend fun ByteReadChannel.writeTo(fileSystem: FileSystem, path: Path)
