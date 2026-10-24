
package coil3.network.httpengine

import android.net.http.HttpEngine
import android.net.http.UploadDataProvider
import android.net.http.UploadDataSink
import androidx.annotation.RequiresApi
import coil3.network.NetworkClient
import coil3.network.NetworkRequest
import coil3.network.NetworkRequestBody
import coil3.network.NetworkResponse
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlin.coroutines.resumeWithException
import kotlin.time.TimeSource
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer

@RequiresApi(34)
class HttpEngineNetworkClient(
    private val httpEngine: HttpEngine,
    private val executor: Executor,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : NetworkClient {

    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ): T {
        val requestBody = request.body?.readByteArray()
        val requestMillis = System.currentTimeMillis()
        val requestTimeMark = timeSource.markNow()

        val response = suspendCancellableCoroutine { continuation ->
            val callback = HttpEngineUrlRequestCallback(
                continuation = continuation,
                executor = executor,
                initialUrl = request.url,
                hasSensitiveRequestData = request.body != null ||
                    request.headers.asMap().keys.any { header ->
                        header !in SAFE_CROSS_ORIGIN_REDIRECT_HEADERS
                    },
                requestMillis = requestMillis,
                requestTimeMark = requestTimeMark,
            )

            try {
                val builder = httpEngine.newUrlRequestBuilder(
                    request.url,
                    executor,
                    callback,
                ).setHttpMethod(request.method)

                for ((key, values) in request.headers.asMap()) {
                    for (value in values) {
                        builder.addHeader(key, value)
                    }
                }

                if (requestBody != null) {
                    if (request.headers["Content-Type"] == null) {
                        builder.addHeader("Content-Type", "application/octet-stream")
                    }
                    builder.setUploadDataProvider(
                        ByteArrayUploadDataProvider(requestBody),
                        executor,
                    )
                }

                val urlRequest = builder.build()
                val requestLock = Any()

                continuation.invokeOnCancellation {
                    try {
                        executor.execute {
                            synchronized(requestLock) {
                                urlRequest.cancel()
                            }
                        }
                    } catch (_: RuntimeException) {}
                }

                try {
                    executor.execute {
                        synchronized(requestLock) {
                            try {
                                urlRequest.start()
                            } catch (exception: RuntimeException) {
                                if (continuation.isActive) {
                                    continuation.resumeWithException(exception)
                                }
                            }
                        }
                    }
                } catch (exception: RuntimeException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
            } catch (exception: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
        }

        return try {
            block(response)
        } finally {
            response.body?.close()
        }
    }

    private class ByteArrayUploadDataProvider(
        private val data: ByteArray,
    ) : UploadDataProvider() {

        private var offset = 0

        override fun getLength(): Long = data.size.toLong()

        override fun read(
            uploadDataSink: UploadDataSink,
            byteBuffer: ByteBuffer,
        ) {
            try {
                val byteCount = minOf(data.size - offset, byteBuffer.remaining())
                if (byteCount > 0) {
                    byteBuffer.put(data, offset, byteCount)
                    offset += byteCount
                }

                uploadDataSink.onReadSucceeded(false)
            } catch (e: Exception) {
                uploadDataSink.onReadError(e)
            }
        }

        override fun rewind(uploadDataSink: UploadDataSink) {
            offset = 0
            uploadDataSink.onRewindSucceeded()
        }
    }
}

// Headers that are safe to forward to a different origin. Treat unknown/custom headers as
// potentially sensitive because they may carry credentials.
private val SAFE_CROSS_ORIGIN_REDIRECT_HEADERS = setOf(
    "accept",
    "accept-encoding",
    "accept-language",
    "cache-control",
    "pragma",
    "user-agent",
)

private suspend fun NetworkRequestBody.readByteArray(): ByteArray {
    val buffer = Buffer()
    writeTo(buffer)
    return buffer.readByteArray()
}
