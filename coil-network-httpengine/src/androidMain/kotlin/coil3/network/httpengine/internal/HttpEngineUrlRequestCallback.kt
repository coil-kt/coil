package coil3.network.httpengine.internal

import android.net.http.HttpException
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import androidx.annotation.RequiresApi
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import coil3.network.httpengine.HttpEngineRedirectPolicy
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import kotlin.coroutines.resumeWithException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import okio.BufferedSink
import okio.FileSystem
import okio.IOException
import okio.Path

@RequiresApi(34)
@OptIn(ExperimentalTime::class)
internal class HttpEngineUrlRequestCallback(
    private val continuation: CancellableContinuation<NetworkResponse>,
    private val networkRequest: NetworkRequest,
    private val executor: Executor,
    private val clock: Clock,
    private val redirectPolicy: HttpEngineRedirectPolicy,
) : UrlRequest.Callback {
    @Volatile private var requestMillis = 0L
    @Volatile private var responseBody: HttpEngineResponseBody? = null

    fun start(request: UrlRequest) {
        requestMillis = clock.now().toEpochMilliseconds()
        request.start()
    }

    override fun onRedirectReceived(
        request: UrlRequest,
        info: UrlResponseInfo,
        newLocationUrl: String,
    ) {
        try {
            redirectPolicy.checkRedirect(networkRequest, info.url, newLocationUrl)
        } catch (exception: IOException) {
            rejectRedirect(request, exception)
            return
        }
        request.followRedirect()
    }

    private fun rejectRedirect(request: UrlRequest, exception: IOException) {
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
        try {
            request.cancel()
        } catch (_: RuntimeException) {}
    }

    override fun onResponseStarted(
        request: UrlRequest,
        info: UrlResponseInfo,
    ) {
        val headersBuilder = NetworkHeaders.Builder()
        for ((key, value) in info.headers.asList) {
            headersBuilder.add(key, value)
        }

        val responseBody = HttpEngineResponseBody(request, executor)
        val networkResponse = NetworkResponse(
            code = info.httpStatusCode,
            requestMillis = requestMillis,
            responseMillis = clock.now().toEpochMilliseconds(),
            headers = headersBuilder.build(),
            body = responseBody,
            delegate = info,
        )
        this.responseBody = responseBody

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
    ) = fail(error)

    override fun onCanceled(
        request: UrlRequest,
        info: UrlResponseInfo?,
    ) = fail(IOException("HttpEngine request canceled"))

    private fun fail(exception: IOException) {
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
    private val channel = Channel<ByteBuffer>(capacity = 1)
    private val consumed = atomic(false)

    override suspend fun writeTo(sink: BufferedSink) {
        consume { sink.write(it) }
    }

    override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
        if (fileSystem === FileSystem.SYSTEM) {
            FileOutputStream(path.toFile()).use { output ->
                val fileChannel = output.channel
                consume { buffer ->
                    while (buffer.hasRemaining()) {
                        fileChannel.write(buffer)
                    }
                }
            }
        } else {
            fileSystem.write(path) { writeTo(this) }
        }
    }

    private suspend inline fun consume(write: (ByteBuffer) -> Unit) {
        check(consumed.compareAndSet(expect = false, update = true)) {
            "HttpEngine response body can only be read once"
        }

        val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
        while (true) {
            buffer.clear()
            scheduleRead(buffer)
            val result = channel.receiveCatching()
            result.exceptionOrNull()?.let { throw it }
            val chunk = result.getOrNull() ?: return
            chunk.flip()
            write(chunk)
        }
    }

    override fun close() {
        channel.close(IOException("HttpEngine response body closed"))
        try {
            executor.execute(request::cancel)
        } catch (_: RuntimeException) {}
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun scheduleRead(buffer: ByteBuffer) {
        try {
            executor.execute {
                if (channel.isClosedForSend || request.isDone) return@execute
                try {
                    request.read(buffer)
                } catch (exception: RuntimeException) {
                    channel.close(exception)
                }
            }
        } catch (exception: RuntimeException) {
            channel.close(exception)
        }
    }

    fun onReadCompleted(buffer: ByteBuffer) {
        channel.trySend(buffer)
    }

    fun onSucceeded() {
        channel.close()
    }

    fun onFailed(exception: IOException) {
        channel.close(exception)
    }

    private companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
