package coil3.network.httpengine

import android.net.http.HttpException
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import androidx.annotation.RequiresApi
import coil3.network.NetworkHeaders
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.Executor
import kotlin.coroutines.resumeWithException
import kotlin.time.TimeMark
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.Channel
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.buffer

@RequiresApi(34)
internal class HttpEngineUrlRequestCallback(
    private val continuation: CancellableContinuation<NetworkResponse>,
    private val executor: Executor,
    initialUrl: String,
    private val hasSensitiveRequestData: Boolean,
    private val requestMillis: Long,
    private val requestTimeMark: TimeMark,
) : UrlRequest.Callback {

    private var responseBody: HttpEngineResponseBody? = null
    private var currentUrl: String = initialUrl

    override fun onRedirectReceived(
        request: UrlRequest,
        info: UrlResponseInfo,
        newLocationUrl: String,
    ) {
        val redirectUrl = resolveRedirect(currentUrl, newLocationUrl)
        if (redirectUrl == null) {
            rejectRedirect(request, IOException("HttpEngine returned an invalid redirect URL"))
            return
        }

        // UrlRequest.followRedirect() doesn't let us remove headers or the upload body from the
        // redirected request. Do not forward potentially sensitive request data to another origin.
        // Unknown/custom headers are treated as sensitive because they may contain credentials.
        if (hasSensitiveRequestData && !hasSameOrigin(currentUrl, redirectUrl)) {
            rejectRedirect(
                request,
                IOException("Refusing cross-origin redirect with sensitive request data"),
            )
            return
        }

        currentUrl = redirectUrl.toString()
        request.followRedirect()
    }

    private fun resolveRedirect(currentUrl: String, location: String): URI? = try {
        URI(currentUrl).resolve(URI(location)).normalize()
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: java.net.URISyntaxException) {
        null
    }

    private fun hasSameOrigin(currentUrl: String, redirectUrl: URI): Boolean {
        val currentOrigin = origin(try {
            URI(currentUrl)
        } catch (_: Exception) {
            return false
        }) ?: return false
        val redirectOrigin = origin(redirectUrl) ?: return false
        return currentOrigin == redirectOrigin
    }

    private fun origin(uri: URI): Origin? {
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        val port = when {
            uri.port >= 0 -> uri.port
            scheme == "https" -> 443
            else -> 80
        }
        return Origin(scheme, host, port)
    }

    private data class Origin(
        val scheme: String,
        val host: String,
        val port: Int,
    )

    private fun rejectRedirect(request: UrlRequest, exception: IOException) {
        try {
            request.cancel()
        } catch (_: RuntimeException) {}
        if (continuation.isActive) {
            continuation.resumeWithException(exception)
        }
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
