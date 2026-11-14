package coil3.network.httpengine.internal

import android.net.http.HttpEngine
import android.net.http.UploadDataProvider
import android.net.http.UploadDataSink
import androidx.annotation.RequiresApi
import coil3.network.NetworkClient
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.httpengine.HttpEngineRedirectPolicy
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlin.collections.iterator
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer

@RequiresApi(34)
internal class HttpEngineNetworkClient(
    private val httpEngine: HttpEngine,
    private val executor: Executor = Dispatchers.IO.asExecutor(),
    private val clock: Clock = Clock.System,
    private val redirectPolicy: HttpEngineRedirectPolicy = HttpEngineRedirectPolicy.DEFAULT,
) : NetworkClient {

    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ): T {
        val requestBody = request.body?.readBuffer()
        val response = suspendCancellableCoroutine { continuation ->
            val callback = HttpEngineUrlRequestCallback(
                continuation = continuation,
                networkRequest = request,
                executor = executor,
                clock = clock,
                redirectPolicy = redirectPolicy,
            )

            try {
                val urlBuilder = httpEngine
                    .newUrlRequestBuilder(request.url, executor, callback)
                    .setHttpMethod(request.method)

                for ((key, values) in request.headers.asMap()) {
                    for (value in values) {
                        urlBuilder.addHeader(key, value)
                    }
                }

                if (requestBody != null) {
                    if (request.headers["Content-Type"] == null) {
                        urlBuilder.addHeader("Content-Type", "application/octet-stream")
                    }
                    urlBuilder.setUploadDataProvider(
                        BufferUploadDataProvider(requestBody),
                        executor,
                    )
                }

                val urlRequest = urlBuilder.build()
                val requestLock = Any()

                continuation.invokeOnCancellation {
                    try {
                        executor.execute {
                            synchronized(requestLock) {
                                urlRequest.cancel()
                            }
                        }
                    } catch (_: Exception) {}
                }

                executor.execute {
                    synchronized(requestLock) {
                        if (!continuation.isActive) return@synchronized
                        try {
                            callback.start(urlRequest)
                        } catch (exception: Exception) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(exception)
                            }
                        }
                    }
                }
            } catch (exception: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }
        }

        return response.body.use { block(response) }
    }
}

@RequiresApi(34)
private class BufferUploadDataProvider(
    private val data: Buffer,
) : UploadDataProvider() {
    // Buffer.copy() shares the underlying byte arrays.
    private var remaining = data.copy()

    override fun getLength() = data.size

    override fun read(
        uploadDataSink: UploadDataSink,
        byteBuffer: ByteBuffer,
    ) {
        try {
            while (byteBuffer.hasRemaining() && remaining.size > 0) {
                remaining.read(byteBuffer)
            }

            uploadDataSink.onReadSucceeded(false)
        } catch (e: Exception) {
            uploadDataSink.onReadError(e)
        }
    }

    override fun rewind(uploadDataSink: UploadDataSink) {
        remaining = data.copy()
        uploadDataSink.onRewindSucceeded()
    }
}
