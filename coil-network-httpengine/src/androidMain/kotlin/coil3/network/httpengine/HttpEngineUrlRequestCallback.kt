package coil3.network.httpengine

import android.net.http.HttpException
import android.net.http.UrlRequest
import android.net.http.UrlResponseInfo
import androidx.annotation.RequiresApi
import coil3.network.NetworkHeaders
import coil3.network.NetworkResponse
import coil3.network.NetworkResponseBody
import java.net.URI
import java.nio.ByteBuffer
import java.util.Locale
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
    private val executor: Executor,
    initialUrl: String,
    private val hasSensitiveRequestData: Boolean,
    private val requestMillis: Long,
    private val clock: Clock,
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
        val currentUri = try {
            URI(currentUrl)
        } catch (_: Exception) {
            return false
        }
        val currentOrigin = origin(currentUri) ?: return false
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
            responseMillis = clock.now().toEpochMilliseconds(),
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
    private val channel = Channel<ByteBuffer>(capacity = 1)
    private val consumed = atomic(false)

    override suspend fun writeTo(sink: BufferedSink) {
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
            sink.write(chunk)
        }
    }

    override suspend fun writeTo(fileSystem: FileSystem, path: Path) {
        fileSystem.write(path) { writeTo(this) }
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
        private const val BUFFER_SIZE = 16 * 1024
    }
}
