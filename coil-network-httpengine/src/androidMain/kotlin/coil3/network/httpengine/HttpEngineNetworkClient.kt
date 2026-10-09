
package coil3.network.httpengine

import android.net.http.HttpEngine
import android.net.http.HttpException
import android.net.http.UploadDataProvider
import android.net.http.UploadDataSink
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import androidx.annotation.RequiresApi
import coil3.network.NetworkClient
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkRequestBody
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.buffer

@RequiresApi(34)
class HttpEngineNetworkClient(
    private val httpEngine: HttpEngine,
    private val executor: Executor,
) : NetworkClient {

    override suspend fun <T> executeRequest(
        request: NetworkRequest,
        block: suspend (response: NetworkResponse) -> T,
    ): T {
        val requestBody = request.body?.readByteArray()
        val requestMillis = System.currentTimeMillis()

        val response = suspendCancellableCoroutine { continuation ->
            val callback = object : UrlRequest.Callback {
                private var responseBody: HttpEngineResponseBody? = null

                override fun onRedirectReceived(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    newLocationUrl: String,
                ) {
                    request.followRedirect()
                }

                override fun onResponseStarted(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                ) {
                    val body = HttpEngineResponseBody(request, executor)
                    responseBody = body

                    val headersBuilder = NetworkHeaders.Builder()
                    for ((key, value) in info.headers.asList) {
                        headersBuilder.add(key, value)
                    }

                    val networkResponse = NetworkResponse(
                        code = info.httpStatusCode,
                        requestMillis = requestMillis,
                        responseMillis = System.currentTimeMillis(),
                        headers = headersBuilder.build(),
                        body = body,
                        delegate = info,
                    )

                    continuation.resume(networkResponse) { _, value, _ ->
                        value.body?.close()
                    }
                }

                override fun onReadCompleted(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                    byteBuffer: ByteBuffer,
                ) {
                    responseBody?.onReadCompleted(byteBuffer)
                }

                override fun onSucceeded(
                    request: UrlRequest,
                    info: UrlResponseInfo,
                ) {
                    responseBody?.onSucceeded()
                }

                override fun onFailed(
                    request: UrlRequest,
                    info: UrlResponseInfo?,
                    error: HttpException,
                ) {
                    val ioException = IOException(
                        "HttpEngine request failed: ${error.message}",
                        error,
                    )
                    if (continuation.isActive) {
                        continuation.resumeWithException(ioException)
                    } else {
                        responseBody?.onFailed(ioException)
                    }
                }

                override fun onCanceled(
                    request: UrlRequest,
                    info: UrlResponseInfo?,
                ) {
                    val exception = IOException("HttpEngine request canceled")
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    } else {
                        responseBody?.onFailed(exception)
                    }
                }
            }

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
                val canceled = AtomicBoolean(false)

                continuation.invokeOnCancellation {
                    canceled.set(true)
                    try {
                        executor.execute {
                            urlRequest.cancel()
                        }
                    } catch (_: RuntimeException) {}
                }

                try {
                    executor.execute {
                        if (!canceled.get()) {
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

    private class HttpEngineResponseBody(
        private val request: UrlRequest,
        private val executor: Executor,
    ) : NetworkResponseBody {

        private val lock = Any()
        private val channel = Channel<Int>(capacity = 1)
        private val readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
        private val responseBuffer = ByteArray(BUFFER_SIZE)

        @Volatile
        private var closed = false

        @Volatile
        private var readStarted = false

        @Volatile
        private var finished = false

        override suspend fun writeTo(sink: BufferedSink) {
            readBody { byteCount ->
                sink.write(responseBuffer, 0, byteCount)
            }
        }

        override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
            val sink = fileSystem.sink(path).buffer()
            sink.use { sink ->
                writeTo(sink)
            }
        }

        override fun close() {
            synchronized(lock) {
                if (closed) return
                closed = true
                channel.close(IOException("HttpEngine response body closed"))
            }

            try {
                executor.execute {
                    synchronized(lock) {
                        request.cancel()
                    }
                }
            } catch (_: RuntimeException) {}
        }

        private suspend inline fun readBody(crossinline consume: (Int) -> Unit) {
            requestNextRead()
            while (true) {
                val result = channel.receiveCatching()
                if (result.isClosed) {
                    result.exceptionOrNull()?.let { throw it }
                    return
                }

                consume(result.getOrThrow())
                requestNextRead()
            }
        }

        private fun requestNextRead() {
            if (closed || finished || readStarted) return
            readStarted = true

            try {
                executor.execute {
                    synchronized(lock) {
                        if (closed || finished) {
                            readStarted = false
                            return@synchronized
                        }

                        try {
                            readBuffer.clear()
                            request.read(readBuffer)
                        } catch (exception: RuntimeException) {
                            readStarted = false
                            failLocked(IOException("HttpEngine response read failed", exception))
                        }
                    }
                }
            } catch (exception: RuntimeException) {
                readStarted = false
                synchronized(lock) {
                    failLocked(IOException("Unable to schedule HttpEngine response read", exception))
                }
            }
        }

        fun onReadCompleted(buffer: ByteBuffer) {
            synchronized(lock) {
                if (closed) return

                buffer.flip()
                val byteCount = buffer.remaining()
                buffer.get(responseBuffer, 0, byteCount)
                buffer.clear()
                readStarted = false

                val result = channel.trySend(byteCount)
                if (result.isFailure && !result.isClosed) {
                    failLocked(IOException("Unable to queue HttpEngine response data"))
                }
            }
        }

        fun onSucceeded() {
            synchronized(lock) {
                finished = true
                readStarted = false
                channel.close()
            }
        }

        fun onFailed(exception: IOException) {
            synchronized(lock) {
                failLocked(exception)
            }
        }

        private fun failLocked(exception: IOException) {
            finished = true
            readStarted = false
            channel.close(exception)
        }

        private companion object {
            const val BUFFER_SIZE = 16 * 1024
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

private suspend fun NetworkRequestBody.readByteArray(): ByteArray {
    val buffer = Buffer()
    writeTo(buffer)
    return buffer.readByteArray()
}
