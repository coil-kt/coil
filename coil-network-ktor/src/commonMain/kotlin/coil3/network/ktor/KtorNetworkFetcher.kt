package coil3.network.ktor

import coil3.network.CacheStrategy
import coil3.network.NetworkClient
import coil3.network.NetworkFetcher
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.ktor.internal.writeTo
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
import kotlin.jvm.JvmOverloads
import okio.BufferedSink
import okio.FileSystem
import okio.Path

@JvmOverloads
fun KtorNetworkFetcherFactory(
    httpClient: Lazy<HttpClient> = lazy { HttpClient() },
) = KtorNetworkFetcherFactory(
    httpClient = httpClient,
    cacheStrategy = lazy { CacheStrategy() },
)

fun KtorNetworkFetcherFactory(
    httpClient: Lazy<HttpClient>,
    cacheStrategy: Lazy<CacheStrategy>,
) = NetworkFetcher.Factory(
    networkClient = lazy { httpClient.value.asNetworkClient() },
    cacheStrategy = cacheStrategy,
)

fun HttpClient.asNetworkClient(): NetworkClient {
    return KtorNetworkClient(this)
}

@JvmInline
private value class KtorNetworkClient(
    private val httpClient: HttpClient,
) : NetworkClient {
    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ) = httpClient.prepareRequest(request.toHttpRequestBuilder()).execute { response ->
        block(response.toNetworkResponse())
    }
}

private fun NetworkRequest.toHttpRequestBuilder(): HttpRequestBuilder {
    val request = HttpRequestBuilder()
    request.url.takeFrom(url)
    request.method = HttpMethod.parse(method)
    request.headers.takeFrom(headers)
    body?.let { request.setBody(it.readByteArray()) }
    return request
}

private suspend fun HttpResponse.toNetworkResponse(): NetworkResponse {
    return NetworkResponse(
        requestMillis = requestTime.timestamp,
        responseMillis = responseTime.timestamp,
        code = status.value,
        headers = headers.toNetworkHeaders(),
        body = KtorNetworkResponseBody(bodyAsChannel()),
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
) : NetworkResponse.Body {

    override val availableBytes: Int
        get() = channel.availableForRead

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
