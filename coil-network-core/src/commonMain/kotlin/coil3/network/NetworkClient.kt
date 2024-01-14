@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.network

import coil3.annotation.Data
import coil3.annotation.ExperimentalCoilApi
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
    val method: String = "GET",
    val headers: NetworkHeaders = NetworkHeaders.EMPTY,
    val body: BufferedSource? = null,
) {
    fun copy(
        url: String = this.url,
        method: String = this.method,
        headers: NetworkHeaders = this.headers,
        body: BufferedSource? = this.body,
    ) = NetworkRequest(url, method, headers, body)
}

@ExperimentalCoilApi
@Data
class NetworkResponse(
    val delegate: Any,
    val request: NetworkRequest,
    val requestMillis: Long,
    val responseMillis: Long,
    val code: Int = 200,
    val headers: NetworkHeaders = NetworkHeaders.EMPTY,
    val body: Body? = null,
) {
    fun copy(
        delegate: Any = this.delegate,
        request: NetworkRequest = this.request,
        requestMillis: Long = this.requestMillis,
        responseMillis: Long = this.responseMillis,
        code: Int = this.code,
        headers: NetworkHeaders = this.headers,
        body: Body? = this.body,
    ) = NetworkResponse(delegate, request, requestMillis, responseMillis, code, headers, body)

    interface Body : Closeable {
        fun exhausted(): Boolean
        suspend fun writeTo(sink: BufferedSink)
        suspend fun writeTo(fileSystem: FileSystem, path: Path)
    }
}
