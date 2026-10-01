
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
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.Buffer
import okio.Source
import okio.Timeout
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
                        body = NetworkResponseBody(body.buffer()),
                        delegate = info,
                    )

                    continuation.resume(networkResponse) { _, value, _ ->
                        value.body?.close()
                    }

                    try {
                        body.start()
                    } catch (exception: Exception) {
                        body.onFailed(
                            IOException("Unable to start HttpEngine response read", exception),
                        )
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
    ) : Source {

        private sealed interface Event {
            class Data(val data: ByteArray) : Event
            data object Succeeded : Event
            class Failed(val exception: IOException) : Event
        }

        private val events = LinkedBlockingQueue<Event>(1)
        private val readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

        @Volatile
        private var closed = false
        private var currentData: ByteArray? = null
        private var currentOffset = 0

        fun start() {
            if (closed) return
            request.read(readBuffer)
        }

        fun onReadCompleted(buffer: ByteBuffer) {
            if (!closed) {
                buffer.flip()
                val data = ByteArray(buffer.remaining())
                buffer.get(data)
                buffer.clear()
                events.offer(Event.Data(data))
            }
        }

        fun onSucceeded() {
            if (!closed) {
                events.offer(Event.Succeeded)
            }
        }

        fun onFailed(exception: IOException) {
            if (!closed) {
                events.offer(Event.Failed(exception))
            }
        }

        override fun read(sink: Buffer, byteCount: Long): Long {
            if (byteCount == 0L) return 0L

            while (true) {
                val data = currentData
                if (data != null && currentOffset < data.size) {
                    val bytesToRead = minOf(
                        byteCount,
                        (data.size - currentOffset).toLong(),
                    ).toInt()
                    sink.write(data, currentOffset, bytesToRead)
                    currentOffset += bytesToRead
                    return bytesToRead.toLong()
                }

                if (data != null) {
                    currentData = null
                    currentOffset = 0
                    requestNextRead()
                }

                val event = try {
                    events.take()
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    close()
                    throw IOException("Interrupted while reading HttpEngine response body", e)
                }

                when (event) {
                    is Event.Data -> {
                        currentData = event.data
                        currentOffset = 0
                    }
                    Event.Succeeded -> return -1L
                    is Event.Failed -> throw event.exception
                }
            }
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() {
            if (closed) return
            closed = true
            currentData = null
            events.clear()
            events.offer(Event.Failed(IOException("HttpEngine response body closed")))
            try {
                executor.execute {
                    request.cancel()
                }
            } catch (_: RuntimeException) {}
        }

        private fun requestNextRead() {
            if (closed) return
            try {
                executor.execute {
                    if (!closed) {
                        try {
                            request.read(readBuffer)
                        } catch (e: RuntimeException) {
                            onFailed(IOException("HttpEngine response read failed", e))
                        }
                    }
                }
            } catch (e: RuntimeException) {
                onFailed(IOException("Unable to schedule HttpEngine response read", e))
            }
        }

        private companion object {
            const val BUFFER_SIZE = 8192
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
