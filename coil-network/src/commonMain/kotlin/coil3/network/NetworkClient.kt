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
)

@ExperimentalCoilApi
@Data
class NetworkResponse(
    val requestMillis: Long,
    val responseMillis: Long,
    val code: Int = 200,
    val headers: NetworkHeaders = NetworkHeaders.EMPTY,
    val body: Body? = null,
) {
    interface Body : Closeable {
        val availableBytes: Int
        suspend fun writeTo(sink: BufferedSink)
        suspend fun writeTo(fileSystem: FileSystem, path: Path)
    }
}
