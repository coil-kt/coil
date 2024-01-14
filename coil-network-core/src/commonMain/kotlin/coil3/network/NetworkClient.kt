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
    ) = NetworkRequest(
        url = url,
        method = method,
        headers = headers,
        body = body,
    )
}

@ExperimentalCoilApi
@Data
class NetworkResponse(
    val request: NetworkRequest,
    val response: Any,
    val requestMillis: Long,
    val responseMillis: Long,
    val code: Int = 200,
    val headers: NetworkHeaders = NetworkHeaders.EMPTY,
    val body: Body? = null,
) {
    fun copy(
        request: NetworkRequest = this.request,
        response: Any = this.response,
        requestMillis: Long = this.requestMillis,
        responseMillis: Long = this.responseMillis,
        code: Int = this.code,
        headers: NetworkHeaders = this.headers,
        body: Body? = this.body,
    ) = NetworkResponse(
        request = request,
        response = response,
        requestMillis = requestMillis,
        responseMillis = responseMillis,
        code = code,
        headers = headers,
        body = body,
    )

    interface Body : Closeable {
        fun exhausted(): Boolean
        suspend fun writeTo(sink: BufferedSink)
        suspend fun writeTo(fileSystem: FileSystem, path: Path)
    }
}
