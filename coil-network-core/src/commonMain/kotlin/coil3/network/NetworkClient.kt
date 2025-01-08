package coil3.network

import coil3.Extras
import coil3.annotation.Poko
import coil3.network.internal.HTTP_METHOD_GET
import coil3.network.internal.HTTP_RESPONSE_OK
import kotlin.jvm.JvmInline
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString
import okio.FileSystem
import okio.Path

/**
 * An asynchronous HTTP client that executes [NetworkRequest]s and returns [NetworkResponse]s.
 */
interface NetworkClient {
    suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ): T
}

/**
 * Represents an HTTP request.
 *
 * @param url The URL to fetch.
 * @param method The HTTP method.
 * @param headers The HTTP headers.
 * @param body The HTTP request body.
 */
@Poko
class NetworkRequest(
    val url: String,
    val method: String = HTTP_METHOD_GET,
    val headers: NetworkHeaders = NetworkHeaders.EMPTY,
    val body: NetworkRequestBody? = null,
    val extras: Extras = Extras.EMPTY,
) {
    fun copy(
        url: String = this.url,
        method: String = this.method,
        headers: NetworkHeaders = this.headers,
        body: NetworkRequestBody? = this.body,
        extras: Extras = this.extras,
    ) = NetworkRequest(
        url = url,
        method = method,
        headers = headers,
        body = body,
        extras = extras,
    )

    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        url: String,
        method: String = HTTP_METHOD_GET,
        headers: NetworkHeaders = NetworkHeaders.EMPTY,
        body: NetworkRequestBody? = null,
    ) : this(url, method, headers, body)

    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun copy(
        url: String = this.url,
        method: String = this.method,
        headers: NetworkHeaders = this.headers,
        body: NetworkRequestBody? = this.body,
    ) = NetworkRequest(
        url = url,
        method = method,
        headers = headers,
        body = body,
        extras = this.extras,
    )
}

interface NetworkRequestBody {
    suspend fun writeTo(sink: BufferedSink)
}

fun NetworkRequestBody(
    bytes: ByteString,
): NetworkRequestBody = ByteStringNetworkRequestBody(bytes)

@JvmInline
private value class ByteStringNetworkRequestBody(
    private val bytes: ByteString,
) : NetworkRequestBody {

    override suspend fun writeTo(sink: BufferedSink) {
        sink.write(bytes)
    }
}

/**
 * Represents an HTTP response.
 *
 * @param code The HTTP response code.
 * @param requestMillis Timestamp of when the request was sent.
 * @param responseMillis Timestamp of when the response was received.
 * @param headers The HTTP headers.
 * @param body The HTTP response body.
 * @param delegate The underlying response instance.
 *  If executed by OkHttp, this is `okhttp3.Response`.
 *  If executed by Ktor, this is `io.ktor.client.statement.HttpResponse`.
 *  If returned from the cache (or any other method), this is `null`.
 */
@Poko
class NetworkResponse(
    val code: Int = HTTP_RESPONSE_OK,
    val requestMillis: Long = 0L,
    val responseMillis: Long = 0L,
    val headers: NetworkHeaders = NetworkHeaders.EMPTY,
    val body: NetworkResponseBody? = null,
    val delegate: Any? = null,
) {
    fun copy(
        code: Int = this.code,
        requestMillis: Long = this.requestMillis,
        responseMillis: Long = this.responseMillis,
        headers: NetworkHeaders = this.headers,
        body: NetworkResponseBody? = this.body,
        delegate: Any? = this.delegate,
    ) = NetworkResponse(
        code = code,
        requestMillis = requestMillis,
        responseMillis = responseMillis,
        headers = headers,
        body = body,
        delegate = delegate,
    )
}

interface NetworkResponseBody : AutoCloseable {
    suspend fun writeTo(sink: BufferedSink)
    suspend fun writeTo(fileSystem: FileSystem, path: Path)
}

fun NetworkResponseBody(
    source: BufferedSource,
): NetworkResponseBody = SourceResponseBody(source)

@JvmInline
private value class SourceResponseBody(
    private val source: BufferedSource,
) : NetworkResponseBody {

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
