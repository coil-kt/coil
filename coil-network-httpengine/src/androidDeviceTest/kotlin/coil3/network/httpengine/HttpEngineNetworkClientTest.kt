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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Clock
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
import okhttp3.mockwebserver.SocketPolicy
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
        TestResources().use { test ->
            test.server.enqueue(MockResponse().setBody("ok"))
            test.server.start()

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
            val client = HttpEngineNetworkClient(test.engine, test.executor, clock)
            val request = NetworkRequest(
                url = test.server.url("/").toString(),
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
        }
    }

    @Test
    fun responseBodyIsRead() = runTest {
        TestResources(Executors.newFixedThreadPool(4)).use { test ->
            val expectedBody = Random(0).nextBytes(128 * 1024 + 37)
            test.server.enqueue(MockResponse().setBody(Buffer().write(expectedBody)))
            test.server.start()

            val request = NetworkRequest(test.server.url("/").toString())

            val response = test.client.executeRequest(request) {
                val buffer = Buffer()
                it.body!!.writeTo(buffer)
                buffer.readByteArray()
            }

            assertContentEquals(expectedBody, response)
        }
    }

    @Test
    fun responseBodyCanBeWrittenToFileSystem() = runTest {
        TestResources(Executors.newFixedThreadPool(4)).use { test ->
            val expectedBody = Random(0).nextBytes(128 * 1024 + 37)
            test.server.enqueue(MockResponse().setBody(Buffer().write(expectedBody)))
            test.server.start()

            val fileSystem = FakeFileSystem()
            val path = "/response.bin".toPath()
            val request = NetworkRequest(test.server.url("/").toString())

            test.client.executeRequest(request) {
                it.body!!.writeTo(fileSystem, path)
            }

            val actualBody = fileSystem.read(path) { readByteArray() }
            assertContentEquals(expectedBody, actualBody)
        }
    }

    @Test
    fun responseBodyCanOverwriteASystemFile() = runTest {
        // Exercise the FileSystem.SYSTEM optimization that writes directly to a FileChannel.
        TestResources(Executors.newFixedThreadPool(4)).use { test ->
            val path = File.createTempFile("httpengine", ".bin", context.cacheDir).toOkioPath()
            try {
                test.server.start()
                val request = NetworkRequest(test.server.url("/").toString())
                for (size in listOf(128 * 1024 + 37, 37, 0)) {
                    val expectedBody = Random(0).nextBytes(size)
                    test.server.enqueue(MockResponse().setBody(Buffer().write(expectedBody)))
                    FileSystem.SYSTEM.write(path) { write(ByteArray(size + 1024)) }

                    test.client.executeRequest(request) { it.body!!.writeTo(FileSystem.SYSTEM, path) }

                    assertContentEquals(expectedBody, FileSystem.SYSTEM.read(path) { readByteArray() })
                }
            } finally {
                FileSystem.SYSTEM.delete(path)
            }
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

            assertEquals(0, server.requestCount)
        } finally {
            try {
                // Shutdown fails if the request started and is still waiting on its callback.
                engine.shutdown()
            } finally {
                server.shutdown()
            }
        }
    }

    @Test
    fun responseBodyStreamsBeforeCompletionAndCanBeCanceled() = runTest {
        TestResources().use { test ->
            val body = Buffer().write(ByteArray(32 * 1024))
            // Leave the response open so the reader must suspend after consuming these bytes.
            test.server.enqueue(MockResponse().setBody(body).setHeader("Content-Length", 64 * 1024))
            test.server.start()

            val firstWrite = CompletableDeferred<Unit>()
            val sink = object : ForwardingSink(Buffer()) {
                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    firstWrite.complete(Unit)
                }
            }.buffer()
            val request = NetworkRequest(test.server.url("/").toString())
            val reader = launch {
                test.client.executeRequest(request) { it.body!!.writeTo(sink) }
            }

            try {
                firstWrite.await()
                assertTrue(reader.isActive)
            } finally {
                reader.cancelAndJoin()
            }
        }
    }

    @Test
    fun truncatedResponseFailsInsteadOfReturningPartialData() = runTest {
        TestResources().use { test ->
            test.server.enqueue(
                MockResponse()
                    .setBody(Buffer().write(Random(0).nextBytes(32 * 1024)))
                    .setHeader("Content-Length", 64 * 1024)
                    .setSocketPolicy(SocketPolicy.DISCONNECT_AT_END),
            )
            test.server.start()

            val received = Buffer()
            val request = NetworkRequest(test.server.url("/").toString())
            assertFailsWith<IOException> {
                test.client.executeRequest(request) { it.body!!.writeTo(received) }
            }
            assertTrue(received.size > 0, "Failure must occur after receiving response data")
        }
    }

    @Test
    fun requestCanBeCanceledWhileWaitingForHeaders() = runTest {
        TestResources().use { test ->
            test.server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            test.server.start()

            val request = NetworkRequest(test.server.url("/").toString())
            val job = launch(start = CoroutineStart.UNDISPATCHED) {
                test.client.executeRequest(request) { error("Response was not sent") }
            }
            try {
                assertNotNull(test.server.takeRequest(5, SECONDS))
            } finally {
                job.cancelAndJoin()
            }
        }
    }

    @Test
    fun unreadResponseIsClosedWhenConsumerReturnsOrThrows() = runTest {
        for (throwFromConsumer in listOf(false, true)) {
            TestResources().use { test ->
                test.server.enqueue(MockResponse().setBody("unread"))
                test.server.start()

                val request = NetworkRequest(test.server.url("/").toString())
                val failure = IOException("Consumer failed")
                if (throwFromConsumer) {
                    val caught = assertFailsWith<IOException> {
                        test.client.executeRequest(request) { throw failure }
                    }
                    assertSame(failure, caught)
                } else {
                    assertEquals("result", test.client.executeRequest(request) { "result" })
                }
            }
        }
    }

    @Test
    fun requestBodyGetsDefaultContentType() = runTest {
        TestResources().use { test ->
            test.server.enqueue(MockResponse().setBody("ok"))
            test.server.start()

            val requestBody = "request-body".encodeToByteArray()
            val request = NetworkRequest(
                url = test.server.url("/").toString(),
                method = "POST",
                body = NetworkRequestBody(requestBody.toByteString()),
            )

            test.client.executeRequest(request) {
                it.body!!.writeTo(Buffer())
            }

            val recordedRequest = assertNotNull(test.server.takeRequest(5, SECONDS))
            assertEquals("application/octet-stream", recordedRequest.getHeader("Content-Type"))
            assertContentEquals(requestBody, recordedRequest.body.readByteArray())
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
        TestResources().use { test ->
            test.server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "/next"))
            test.server.start()

            val rejection = IllegalStateException("Redirect policy failed")
            val policy = HttpEngineRedirectPolicy { _, _, _ -> throw rejection }
            val client = HttpEngineNetworkClient(test.engine, test.executor, redirectPolicy = policy)

            val failure = assertFailsWith<IllegalStateException> {
                client.executeRequest(NetworkRequest(test.server.url("/").toString())) {
                    error("Redirect policy returned a response")
                }
            }

            assertSame(rejection, failure)
        }
    }

    @Test
    fun requestBodyCanBeRewoundForRedirect() = runTest {
        TestResources().use { test ->
            test.server.start()
            val redirectUrl = test.server.url("/final")
            test.server.enqueue(
                MockResponse()
                    .setResponseCode(307)
                    .setHeader("Location", redirectUrl),
            )
            test.server.enqueue(MockResponse().setBody("ok"))

            val requestBody = Random(0).nextBytes(64 * 1024 + 37)
            val request = NetworkRequest(
                url = test.server.url("/redirect").toString(),
                method = "POST",
                headers = NetworkHeaders.Builder()
                    .set("Content-Type", "text/plain")
                    .build(),
                body = NetworkRequestBody(requestBody.toByteString()),
            )

            test.client.executeRequest(request) {
                val buffer = Buffer()
                it.body!!.writeTo(buffer)
                assertEquals("ok", buffer.readUtf8())
            }

            val firstRequest = assertNotNull(test.server.takeRequest(5, SECONDS))
            val secondRequest = assertNotNull(test.server.takeRequest(5, SECONDS))
            assertEquals("POST", firstRequest.method)
            assertEquals("POST", secondRequest.method)
            assertEquals("text/plain", firstRequest.getHeader("Content-Type"))
            assertEquals("text/plain", secondRequest.getHeader("Content-Type"))
            assertContentEquals(requestBody, firstRequest.body.readByteArray())
            assertContentEquals(requestBody, secondRequest.body.readByteArray())
        }
    }

    private class TestResources(
        val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    ) : AutoCloseable {
        val server = MockWebServer()
        val engine = HttpEngine.Builder(context).build()
        val client = HttpEngineNetworkClient(engine, executor)

        override fun close() {
            try {
                // Run queued cancellation before shutdown, which rejects active native requests.
                executor.submit {}.get(5, SECONDS)
                engine.shutdown()
            } finally {
                try {
                    executor.shutdownNow()
                } finally {
                    server.shutdown()
                }
            }
        }
    }
}
