@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.network

import coil3.annotation.Data
import coil3.annotation.ExperimentalCoilApi
import coil3.network.internal.HTTP_METHOD_GET
import kotlin.jvm.JvmInline
import okio.BufferedSink
import okio.BufferedSource
import okio.Closeable
import okio.FileSystem
import okio.Path

@ExperimentalCoilApi
interface NetworkClient {
    suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ): T
}

@ExperimentalCoilApi
@Data
class NetworkRequest(
    val url: String,
    val method: String = HTTP_METHOD_GET,
    val headers: NetworkHeaders = NetworkHeaders.EMPTY,
    val body: NetworkRequestBody? = null,
) {
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
    )
}

@ExperimentalCoilApi
interface NetworkRequestBody : Closeable {
    suspend fun writeTo(sink: BufferedSink)
}

@ExperimentalCoilApi
fun NetworkRequestBody(
    source: BufferedSource,
): NetworkRequestBody = SourceRequestBody(source)

@ExperimentalCoilApi
@JvmInline
private value class SourceRequestBody(
    private val source: BufferedSource,
) : NetworkRequestBody {

    override suspend fun writeTo(sink: BufferedSink) {
        source.readAll(sink)
    }

    override fun close() {
        source.close()
    }
}

@ExperimentalCoilApi
@Data
class NetworkResponse(
    val request: NetworkRequest,
    val code: Int = 200,
    val requestMillis: Long = 0L,
    val responseMillis: Long = 0L,
    val headers: NetworkHeaders = NetworkHeaders.EMPTY,
    val body: NetworkResponseBody? = null,
    val delegate: Any? = null,
) {
    fun copy(
        request: NetworkRequest = this.request,
        code: Int = this.code,
        requestMillis: Long = this.requestMillis,
        responseMillis: Long = this.responseMillis,
        headers: NetworkHeaders = this.headers,
        body: NetworkResponseBody? = this.body,
        delegate: Any? = this.delegate,
    ) = NetworkResponse(
        request = request,
        code = code,
        requestMillis = requestMillis,
        responseMillis = responseMillis,
        headers = headers,
        body = body,
        delegate = delegate,
    )
}

@ExperimentalCoilApi
interface NetworkResponseBody : Closeable {
    suspend fun writeTo(sink: BufferedSink)
    suspend fun writeTo(fileSystem: FileSystem, path: Path)
}

@ExperimentalCoilApi
fun NetworkResponseBody(
    source: BufferedSource,
): NetworkResponseBody = SourceResponseBody(source)

@ExperimentalCoilApi
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
