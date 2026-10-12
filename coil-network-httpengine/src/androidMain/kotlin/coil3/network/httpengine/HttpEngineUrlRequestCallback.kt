package coil3.network.httpengine

import android.net.http.HttpException
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import androidx.annotation.RequiresApi
import coil3.annotation.InternalCoilApi
import coil3.network.NetworkHeaders
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlin.coroutines.resumeWithException
import kotlin.time.TimeMark
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.Channel
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.buffer

@InternalCoilApi
@RequiresApi(34)
class HttpEngineUrlRequestCallback(
    private val continuation: CancellableContinuation<NetworkResponse>,
    private val executor: Executor,
    private val requestMillis: Long,
    private val requestTimeMark: TimeMark,
) : UrlRequest.Callback {

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
            responseMillis = requestMillis + requestTimeMark.elapsedNow().inWholeMilliseconds,
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
        if (continuation.isActive) {
            continuation.resumeWithException(error)
        } else {
            responseBody?.onFailed(error)
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

@RequiresApi(34)
private class HttpEngineResponseBody(
    private val request: UrlRequest,
    private val executor: Executor,
) : NetworkResponseBody {

    private val lock = Any()
    private val channel = Channel<ByteBuffer>(capacity = 1)
    private val readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

    @Volatile
    private var closed = false

    @Volatile
    private var readStarted = false

    @Volatile
    private var finished = false

    override suspend fun writeTo(sink: BufferedSink) {
        readBody { buffer ->
            sink.buffer.write(buffer)
            sink.emitCompleteSegments()
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

    private suspend inline fun readBody(crossinline consume: (ByteBuffer) -> Unit) {
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
            readStarted = false
            val result = channel.trySend(buffer)
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
