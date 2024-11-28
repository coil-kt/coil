package coil3

import coil3.fetch.Fetcher
import coil3.request.ImageRequest
import coil3.test.utils.FakeImage
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem

class RealImageLoaderTest : RobolectricTest() {

    private lateinit var fileSystem: FakeFileSystem
    private lateinit var imageLoader: ImageLoader

    @BeforeTest
    fun before() {
        fileSystem = FakeFileSystem()
        imageLoader = ImageLoader.Builder(context)
            .diskCache(null)
            .fileSystem(fileSystem)
            .build()
    }

    @AfterTest
    fun after() {
        fileSystem.checkNoOpenFiles()
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/933 */
    @Test
    fun executeIsCancelledIfScopeIsCancelled() = runTest {
        val isCancelled = MutableStateFlow(false)

        val job = backgroundScope.launch {
            val request = ImageRequest.Builder(context)
                .data(Unit)
                .coroutineContext(EmptyCoroutineContext)
                .fetcherFactory<Unit> { _, _, _ ->
                    // Use a custom fetcher that suspends until cancellation.
                    Fetcher { awaitCancellation() }
                }
                .listener(
                    onCancel = {
                        isCancelled.value = true
                    },
                )
                .build()
            imageLoader.execute(request)
        }

        testScheduler.runCurrent()

        assertTrue(job.isActive)
        assertFalse(isCancelled.value)

        job.cancel()

        // Suspend until the request is cancelled.
        isCancelled.first { it }
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/2119 */
    @Test
    fun imageLoaderPlaceholderIsRespected() = runTest {
        val expected = FakeImage()
        var actual: Image? = null
        val imageLoader = ImageLoader.Builder(context)
            .placeholder(expected)
            .build()
        val request = ImageRequest.Builder(context)
            .data(Unit)
            .target(
                onStart = {
                    actual = it
                    throw CancellationException()
                },
            )
            .build()

        try {
            imageLoader.execute(request)
        } catch (_: CancellationException) {}

        assertSame(expected, actual)
    }
}
