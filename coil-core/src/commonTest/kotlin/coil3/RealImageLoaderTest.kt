package coil3

import coil3.fetch.Fetcher
import coil3.request.ImageRequest
import coil3.test.utils.FakeImage
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.fakefilesystem.FakeFileSystem

@OptIn(ExperimentalCoroutinesApi::class)
class RealImageLoaderTest : RobolectricTest() {

    private lateinit var mainDispatcher: TestDispatcher
    private lateinit var fileSystem: FakeFileSystem
    private lateinit var imageLoader: ImageLoader

    @BeforeTest
    fun before() {
        mainDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
        fileSystem = FakeFileSystem()
        imageLoader = ImageLoader.Builder(context)
            .diskCache(null)
            .fileSystem(fileSystem)
            .build()
    }

    @AfterTest
    fun after() {
        Dispatchers.resetMain()
        fileSystem.checkNoOpenFiles()
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/933 */
    @Test
    fun executeIsCancelledIfScopeIsCancelled() = runTest {
        val isCancelled = MutableStateFlow(false)

        val scope = CoroutineScope(mainDispatcher)
        scope.launch {
            val request = ImageRequest.Builder(context)
                .data(Unit)
                .dispatcher(mainDispatcher)
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

        assertTrue(scope.isActive)
        assertFalse(isCancelled.value)

        scope.cancel()

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
