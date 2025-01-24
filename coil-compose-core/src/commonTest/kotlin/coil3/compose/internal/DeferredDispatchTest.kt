package coil3.compose.internal

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.test.utils.FakeImage
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import okio.Buffer

class DeferredDispatchTest : RobolectricTest() {
    private val testDispatcher = TestCoroutineDispatcher()

    @Test
    fun `does not dispatch if dispatcher does not change`() = runTest {
        withContext(testDispatcher) {
            launchWithDeferredDispatch {
                assertEquals(1, testDispatcher.dispatchCount)

                delay(10.milliseconds)
                withContext(EmptyCoroutineContext) {}

                assertEquals(1, testDispatcher.dispatchCount)
            }.join()
        }
    }

    @Test
    fun `does dispatch if dispatcher changes`() = runTest {
        withContext(testDispatcher) {
            launchWithDeferredDispatch {
                assertEquals(1, testDispatcher.dispatchCount)

                delay(10.milliseconds)
                withContext(Dispatchers.Default) {}

                assertEquals(2, testDispatcher.dispatchCount)
            }.join()
        }
    }

    @Test
    fun `image loader does not dispatch if dispatcher does not change`() = runTest {
        withContext(testDispatcher) {
            launchWithDeferredDispatch {
                assertEquals(1, testDispatcher.dispatchCount)

                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(Unit)
                    .fetcherFactory(TestFetcher.Factory())
                    .decoderFactory(TestDecoder.Factory())
                    .coroutineContext(EmptyCoroutineContext)
                    .build()
                val result = imageLoader.execute(request)

                assertIs<SuccessResult>(result)
                assertEquals(1, testDispatcher.dispatchCount)
            }.join()
        }
    }

    @Test
    fun `image loader does dispatch if dispatcher changes`() = runTest {
        withContext(testDispatcher) {
            launchWithDeferredDispatch {
                assertEquals(1, testDispatcher.dispatchCount)

                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(Unit)
                    .fetcherFactory(TestFetcher.Factory())
                    .decoderFactory(TestDecoder.Factory())
                    .decoderCoroutineContext(Dispatchers.Default)
                    .build()
                val result = imageLoader.execute(request)

                assertIs<SuccessResult>(result)
                assertEquals(2, testDispatcher.dispatchCount)
            }.join()
        }
    }

    private class TestCoroutineDispatcher : CoroutineDispatcher() {
        private val _dispatchCount = atomic(0)
        val dispatchCount: Int by _dispatchCount

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            _dispatchCount.update { it + 1 }
            block.run()
        }
    }

    private class TestFetcher(
        private val options: Options,
    ) : Fetcher {

        override suspend fun fetch(): FetchResult {
            return SourceFetchResult(
                source = ImageSource(Buffer(), options.fileSystem),
                mimeType = null,
                dataSource = DataSource.MEMORY,
            )
        }

        class Factory : Fetcher.Factory<Unit> {
            override fun create(
                data: Unit,
                options: Options,
                imageLoader: ImageLoader,
            ): Fetcher = TestFetcher(options)
        }
    }

    private class TestDecoder(
        private val options: Options,
    ) : Decoder {

        override suspend fun decode(): DecodeResult {
            return DecodeResult(
                image = FakeImage(),
                isSampled = false,
            )
        }

        class Factory : Decoder.Factory {
            override fun create(
                result: SourceFetchResult,
                options: Options,
                imageLoader: ImageLoader,
            ): Decoder = TestDecoder(options)
        }
    }
}
