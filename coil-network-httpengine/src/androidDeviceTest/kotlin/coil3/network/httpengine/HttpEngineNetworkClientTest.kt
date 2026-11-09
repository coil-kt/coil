package coil3.network.httpengine

import android.net.http.HttpEngine
import androidx.test.filters.SdkSuppress
import coil3.network.NetworkHeaders
import coil3.network.NetworkRequest
import coil3.network.NetworkRequestBody
import coil3.network.httpengine.internal.HttpEngineNetworkClient
import coil3.test.utils.context
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.BufferedSink
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.ForwardingSink
import okio.IOException
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem

@SdkSuppress(minSdkVersion = 34)
@OptIn(ExperimentalTime::class)
class HttpEngineNetworkClientTest {

    @Test
    fun requestAndResponseTimestampsUseClock() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            server.enqueue(MockResponse().setBody("ok"))
            server.start()

            val sent = Instant.parse("2026-01-01T00:00:00Z")
            val received = Instant.parse("2026-01-01T00:00:01Z")
            var bodyWritten = false
            val clock = object : Clock {
                private val times = listOf(sent, received).iterator()

                override fun now(): Instant {
                    check(bodyWritten)
                    return times.next()
                }
            }
            val client = HttpEngineNetworkClient(engine, executor, clock)
            val request = NetworkRequest(
                url = server.url("/").toString(),
                method = "POST",
                body = object : NetworkRequestBody {
                    override suspend fun writeTo(sink: BufferedSink) {
                        sink.writeUtf8("request")
                        bodyWritten = true
                    }
                },
            )

            client.executeRequest(request) {
                it.body!!.writeTo(Buffer())
                assertEquals(sent.toEpochMilliseconds(), it.requestMillis)
                assertEquals(received.toEpochMilliseconds(), it.responseMillis)
            }
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun responseBodyIsRead() = runTest {
        val server = MockWebServer()
        val executor = Executors.newFixedThreadPool(4)
        val engine = HttpEngine.Builder(context).build()
        try {
            val expectedBody = ByteArray(128 * 1024) { it.toByte() }
            server.enqueue(MockResponse().setBody(Buffer().write(expectedBody)))
            server.start()

            val client = HttpEngineNetworkClient(engine, executor)
            val request = NetworkRequest(server.url("/").toString())

            val response = client.executeRequest(request) {
                val buffer = Buffer()
                it.body!!.writeTo(buffer)
                buffer.readByteArray()
            }

            assertContentEquals(expectedBody, response)
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun responseBodyCanBeWrittenToFileSystem() = runTest {
        val server = MockWebServer()
        val executor = Executors.newFixedThreadPool(4)
        val engine = HttpEngine.Builder(context).build()
        try {
            val expectedBody = ByteArray(128 * 1024 + 37) { (it * 31).toByte() }
            server.enqueue(MockResponse().setBody(Buffer().write(expectedBody)))
            server.start()

            val fileSystem = FakeFileSystem()
            val path = "/response.bin".toPath()
            val request = NetworkRequest(server.url("/").toString())

            HttpEngineNetworkClient(engine, executor).executeRequest(request) {
                it.body!!.writeTo(fileSystem, path)
            }

            val actualBody = fileSystem.read(path) { readByteArray() }
            assertContentEquals(expectedBody, actualBody)
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun responseBodyCanOverwriteASystemFile() = runTest {
        // Exercise the FileSystem.SYSTEM optimization that writes directly to a FileChannel.
        val server = MockWebServer()
        val executor = Executors.newFixedThreadPool(4)
        val engine = HttpEngine.Builder(context).build()
        val path = File.createTempFile("httpengine", ".bin", context.cacheDir).toOkioPath()
        try {
            server.start()
            val client = HttpEngineNetworkClient(engine, executor)
            val request = NetworkRequest(server.url("/").toString())
            for (size in listOf(128 * 1024 + 37, 37, 0)) {
                val expectedBody = ByteArray(size) { (it * 31).toByte() }
                server.enqueue(MockResponse().setBody(Buffer().write(expectedBody)))
                FileSystem.SYSTEM.write(path) { write(ByteArray(size + 1024)) }

                client.executeRequest(request) { it.body!!.writeTo(FileSystem.SYSTEM, path) }

                assertContentEquals(expectedBody, FileSystem.SYSTEM.read(path) { readByteArray() })
            }
        } finally {
            FileSystem.SYSTEM.delete(path)
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun requestCanceledBeforeStartDoesNotStart() = runTest {
        val server = MockWebServer()
        val engine = HttpEngine.Builder(context).build()
        try {
            server.enqueue(MockResponse().setBody("unexpected"))
            server.start()

            val tasks = LinkedBlockingQueue<Runnable>()
            val client = HttpEngineNetworkClient(engine, Executor { tasks.add(it) })
            val request = NetworkRequest(server.url("/").toString())
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                client.executeRequest(request) { error("Cancelled request returned a response") }
            }

            val start = tasks.remove()
            job.cancelAndJoin()
            // Run cancellation before the queued start, as a concurrent executor can do.
            tasks.remove().run()
            start.run()

            // Shutdown fails if the request started and is still waiting on its callback.
            engine.shutdown()
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun responseBodyStreamsBeforeCompletionAndCanBeCanceled() = runTest(timeout = 10.seconds) {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            val body = Buffer().write(ByteArray(32 * 1024))
            // Leave the response open so the reader must suspend after consuming these bytes.
            server.enqueue(MockResponse().setBody(body).setHeader("Content-Length", 64 * 1024))
            server.start()

            val firstWrite = CompletableDeferred<Unit>()
            val sink = object : ForwardingSink(Buffer()) {
                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    firstWrite.complete(Unit)
                }
            }.buffer()
            val client = HttpEngineNetworkClient(engine, executor)
            val request = NetworkRequest(server.url("/").toString())
            val reader = launch {
                client.executeRequest(request) { it.body!!.writeTo(sink) }
            }

            firstWrite.await()
            assertTrue(reader.isActive)
            reader.cancelAndJoin()
        } finally {
            try {
                // Let the cancellation queued by close() run before shutting down the engine.
                executor.submit {}.get(5, SECONDS)
                engine.shutdown()
            } finally {
                executor.shutdownNow()
                server.shutdown()
            }
        }
    }

    @Test
    fun requestBodyIsUploaded() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            server.enqueue(MockResponse().setBody("ok"))
            server.start()

            val requestBody = "request-body".encodeToByteArray()
            val request = NetworkRequest(
                url = server.url("/").toString(),
                method = "POST",
                headers = NetworkHeaders.Builder()
                    .set("Content-Type", "text/plain")
                    .build(),
                body = NetworkRequestBody(requestBody.toByteString()),
            )

            HttpEngineNetworkClient(engine, executor).executeRequest(request) {
                it.body!!.writeTo(Buffer())
            }

            val recordedRequest = server.takeRequest()
            assertEquals("POST", recordedRequest.method)
            assertEquals("text/plain", recordedRequest.getHeader("Content-Type"))
            assertContentEquals(requestBody, recordedRequest.body.readByteArray())
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun requestBodyGetsDefaultContentType() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            server.enqueue(MockResponse().setBody("ok"))
            server.start()

            val requestBody = "request-body".encodeToByteArray()
            val request = NetworkRequest(
                url = server.url("/").toString(),
                method = "POST",
                body = NetworkRequestBody(requestBody.toByteString()),
            )

            HttpEngineNetworkClient(engine, executor).executeRequest(request) {
                it.body!!.writeTo(Buffer())
            }

            val recordedRequest = server.takeRequest()
            assertEquals("application/octet-stream", recordedRequest.getHeader("Content-Type"))
            assertContentEquals(requestBody, recordedRequest.body.readByteArray())
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun redirectPolicyCanRejectALaterHop() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        var engineShutDown = false
        try {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/next"))
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/blocked"))
            server.enqueue(MockResponse().setBody("unexpected"))
            server.start()

            val start = server.url("/start").toString()
            val next = server.url("/next").toString()
            val blocked = server.url("/blocked").toString()
            val request = NetworkRequest(start)
            val hops = mutableListOf<Pair<String, String>>()
            val policy = HttpEngineRedirectPolicy { originalRequest, fromUrl, toUrl ->
                assertSame(request, originalRequest)
                hops += fromUrl to toUrl
                toUrl != blocked
            }
            val client = HttpEngineNetworkClient(engine, executor, redirectPolicy = policy)

            withContext(Dispatchers.Unconfined) {
                assertFailsWith<IOException> {
                    client.executeRequest(request) { it.body!!.writeTo(Buffer()) }
                }
                // Run on the callback thread to catch failures delivered before native teardown.
                engine.shutdown()
                engineShutDown = true
            }

            assertEquals(listOf(start to next, next to blocked), hops)
            assertEquals(2, server.requestCount)
        } finally {
            try {
                if (!engineShutDown) {
                    executor.submit {}.get(5, SECONDS)
                    engine.shutdown()
                }
            } finally {
                executor.shutdownNow()
                server.shutdown()
            }
        }
    }

    @Test
    fun redirectPolicyRuntimeExceptionIsUnwrapped() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/next"))
            server.start()

            val rejection = IllegalStateException("Redirect policy failed")
            val policy = HttpEngineRedirectPolicy { _, _, _ -> throw rejection }
            val client = HttpEngineNetworkClient(engine, executor, redirectPolicy = policy)

            val failure = assertFailsWith<IllegalStateException> {
                client.executeRequest(NetworkRequest(server.url("/").toString())) {
                    error("Redirect policy returned a response")
                }
            }

            assertSame(rejection, failure)
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }

    @Test
    fun requestBodyCanBeRewoundForRedirect() = runTest {
        val server = MockWebServer()
        val executor = Executors.newSingleThreadExecutor()
        val engine = HttpEngine.Builder(context).build()
        try {
            server.start()
            val redirectUrl = server.url("/final")
            server.enqueue(
                MockResponse()
                    .setResponseCode(307)
                    .setHeader("Location", redirectUrl),
            )
            server.enqueue(MockResponse().setBody("ok"))

            val requestBody = kotlin.random.Random(0).nextBytes(64 * 1024 + 37)
            val request = NetworkRequest(
                url = server.url("/redirect").toString(),
                method = "POST",
                headers = NetworkHeaders.Builder()
                    .set("Content-Type", "text/plain")
                    .build(),
                body = NetworkRequestBody(requestBody.toByteString()),
            )

            HttpEngineNetworkClient(engine, executor).executeRequest(request) {
                val buffer = Buffer()
                it.body!!.writeTo(buffer)
                assertEquals("ok", buffer.readUtf8())
            }

            val firstRequest = server.takeRequest()
            val secondRequest = server.takeRequest()
            assertEquals("POST", firstRequest.method)
            assertEquals("POST", secondRequest.method)
            assertContentEquals(requestBody, firstRequest.body.readByteArray())
            assertContentEquals(requestBody, secondRequest.body.readByteArray())
        } finally {
            engine.shutdown()
            executor.shutdownNow()
            server.shutdown()
        }
    }
}
