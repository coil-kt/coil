package coil3.compose

import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.BitmapImage
import coil3.EventListener
import coil3.Extras
import coil3.ImageLoader
import coil3.compose.AsyncImagePainter.State
import coil3.compose.core.test.R
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import coil3.test.utils.ComposeTestActivity
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AsyncImagePainterTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComposeTestActivity>()

    private lateinit var fetcher: FakeNetworkFetcher.Factory
    private lateinit var requestTracker: ImageLoaderIdlingResource
    private lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        requestTracker = ImageLoaderIdlingResource()
        imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .eventListener(requestTracker)
            .components {
                add(FakeNetworkFetcher.Factory())
            }
            .build()
        composeTestRule.registerIdlingResource(requestTracker)
    }

    @After
    fun after() {
        composeTestRule.unregisterIdlingResource(requestTracker)
    }

    @Test
    fun basicLoad_http() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = "https://example.com/image",
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 166.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(166.dp)
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun basicLoad_http_drawScopeSizeResolver() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://example.com/image")
                        .size(rememberDrawScopeSizeResolver())
                        .build(),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 166.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        assertLoadedBitmapSize(128.dp, 166.dp)

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(166.dp)
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun basicLoad_drawableId() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = R.drawable.sample,
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 166.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp, tolerance = 1.dp)
            .assertHeightIsEqualTo(166.dp, tolerance = 1.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun basicLoad_drawableId_drawScopeSizeResolver() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(R.drawable.sample)
                        .size(rememberDrawScopeSizeResolver())
                        .build(),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 166.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        assertLoadedBitmapSize(128.dp, 166.dp)

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp, tolerance = 1.dp)
            .assertHeightIsEqualTo(166.dp, tolerance = 1.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun basicLoad_drawableUri() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = resourceUri(R.drawable.sample),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 166.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(166.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun basicLoad_drawableUri_drawScopeSizeResolver() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(resourceUri(R.drawable.sample))
                        .size(rememberDrawScopeSizeResolver())
                        .build(),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 166.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        assertLoadedBitmapSize(128.dp, 166.dp)

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(166.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun basicLoad_customImageLoader() {
        var requestCompleted by mutableStateOf(false)
        var requestThrowable: Throwable? = null

        // Build a custom ImageLoader with an EventListener.
        val eventListener = object : EventListener() {
            override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                requestCompleted = true
            }

            override fun onError(request: ImageRequest, result: ErrorResult) {
                requestThrowable = result.throwable
            }
        }
        val imageLoader = imageLoader.newBuilder()
            .eventListener(eventListener)
            .build()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = "https://example.com/image",
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier.size(128.dp, 166.dp),
            )
        }

        composeTestRule.waitForIdle()
        // Wait for the event listener to run.
        composeTestRule.waitUntil(10_000) {
            requestThrowable?.let { throw it }
            requestCompleted
        }
    }

    @Test
    fun basicLoad_switchData() {
        assumeSupportsCaptureToImage()

        var data by mutableStateOf("https://example.com/image")

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = data,
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete(finishedRequests = 1)

        // Assert that the content is completely red.
        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample, threshold = 0.75)

        // Now switch the data URI to the blue drawable.
        data = "https://example.com/blue"

        waitForRequestComplete(finishedRequests = 2)

        // Assert that the content is completely blue.
        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.blue_rectangle)
    }

    @Test
    fun basicLoad_changeSize() = runTest {
        val states = Channel<State>()
        var size by mutableStateOf(128.dp)

        composeTestRule.setContent {
            val painter = rememberAsyncImagePainter(
                model = "https://example.com/image",
                imageLoader = imageLoader,
            )

            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .testTag(Image),
            )

            LaunchedEffect(painter) {
                painter.state
                    .filter { it is State.Success || it is State.Error }
                    .onCompletion { states.cancel() }
                    .collect(states::send)
            }
        }

        // Await the first load.
        assertNotNull(states.receive())

        // Now change the size.
        size = 256.dp
        composeTestRule.awaitIdle()

        // Await any potential subsequent load (which shouldn't come).
        val result = withTimeoutOrNull(3_000) { states.receive() }
        assertNull(result)

        // Close the signal channel.
        states.close()
    }

    @Test
    fun basicLoad_noSize() {
        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = "https://example.com/image",
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier.testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsAtLeast(1.dp)
            .assertHeightIsAtLeast(1.dp)
            .assertIsDisplayed()
    }

    @Test
    fun lazyColumn() {
        composeTestRule.setContent {
            LazyColumn(
                modifier = Modifier
                    .size(240.dp, 200.dp),
            ) {
                item {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = "https://example.com/image",
                            imageLoader = imageLoader,
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .testTag(Image),
                    )
                }
            }
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsAtLeast(1.dp)
            .assertHeightIsEqualTo(200.dp)
            .assertIsDisplayed()
    }

    @Test
    fun basicLoad_error() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://example.com/noimage")
                        .error(R.drawable.red_rectangle)
                        .build(),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .testTag(Image)
                    .size(128.dp),
            )
        }

        waitForRequestComplete()

        // Assert that the error drawable was drawn.
        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.red_rectangle)
    }

    @Test
    fun previewPlaceholder() {
        assumeSupportsCaptureToImage()

        val previewHandler = AsyncImagePreviewHandler(ImageRequest::placeholder)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://example.com/image")
                                .placeholder(R.drawable.red_rectangle)
                                .build(),
                            imageLoader = imageLoader,
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .size(128.dp)
                            .testTag(Image),
                    )
                }
            }
        }

        // Assert that we never started the request.
        composeTestRule.waitForIdle()
        assertEquals(0, requestTracker.startedRequests)

        // Assert that the placeholder is showing.
        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.red_rectangle)
    }

    @Test
    fun errorStillHasSize() {
        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = "https://example.com/noimage",
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        // Assert that the layout is in the tree and has the correct size.
        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun data_imagebitmap_throws() {
        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = painterResource(R.drawable.sample),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier.size(128.dp),
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun data_imagevector_throws() {
        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = painterResource(R.drawable.black_rectangle_vector),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier.size(128.dp),
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun data_painter_throws() {
        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ColorPainter(Color.Magenta),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier.size(128.dp),
            )
        }
    }

    @Test
    fun crossfade() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://example.com/image")
                        .placeholder(R.drawable.red_rectangle)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample, threshold = 0.75)
    }

    @Test
    fun fillMaxWidth() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = "https://example.com/image",
                    imageLoader = imageLoader,
                    contentScale = ContentScale.FillWidth,
                ),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(displaySize.width.toDp())
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun columnWithHeight() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = "https://example.com/image",
                            imageLoader = imageLoader,
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .height(100.dp)
                            .testTag(Image),
                    )
                }
            }
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertHeightIsEqualTo(100.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun specifiedSizeResolverExecutesWithoutSpecifiedSize() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://example.com/image")
                        .size(100, 100)
                        .build(),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample, threshold = 0.85)
    }

    @Test
    fun successCallbackIsCalled() {
        val loadingCount = AtomicInteger()
        val successCount = AtomicInteger()
        val errorCount = AtomicInteger()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://example.com/image")
                        .size(100, 100)
                        .build(),
                    imageLoader = imageLoader,
                    onLoading = { loadingCount.getAndIncrement() },
                    onSuccess = { successCount.getAndIncrement() },
                    onError = { errorCount.getAndIncrement() },
                ),
                contentDescription = null,
            )
        }

        waitForRequestComplete()

        assertEquals(1, loadingCount.get())
        assertEquals(1, successCount.get())
        assertEquals(0, errorCount.get())
    }

    @Test
    fun errorCallbackIsCalled() {
        val loadingCount = AtomicInteger()
        val successCount = AtomicInteger()
        val errorCount = AtomicInteger()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://example.com/incorrect_path")
                        .size(100, 100)
                        .build(),
                    imageLoader = imageLoader,
                    onLoading = { loadingCount.getAndIncrement() },
                    onSuccess = { successCount.getAndIncrement() },
                    onError = { errorCount.getAndIncrement() },
                ),
                contentDescription = null,
            )
        }

        waitForRequestComplete()

        assertEquals(1, loadingCount.get())
        assertEquals(0, successCount.get())
        assertEquals(1, errorCount.get())
    }

    @Test
    fun doesNotRecompose() {
        val compositionCount = AtomicInteger()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = "https://example.com/image",
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
            )
            compositionCount.getAndIncrement()
        }

        waitForRequestComplete()

        assertEquals(1, compositionCount.get())
    }

    @Test
    fun restartStartsANewRequestWithSameInputsAndDoesNotRecompose() {
        val compositionCount = AtomicInteger()
        val requestCount = AtomicInteger()

        composeTestRule.setContent {
            val painter = rememberAsyncImagePainter(
                model = "https://example.com/image",
                imageLoader = imageLoader,
            )
            LaunchedEffect(Unit) {
                painter.state.collectLatest { state ->
                    // Launch 3 requests sequentially.
                    if (state is State.Success && requestCount.incrementAndGet() < 3) {
                        painter.restart()
                    }
                }
            }
            Image(
                painter = painter,
                contentDescription = null,
            )
            compositionCount.getAndIncrement()
        }

        waitForRequestComplete(finishedRequests = 3)

        assertEquals(1, compositionCount.get())
        assertSame(requestTracker.requests[0].data, requestTracker.requests[1].data)
        assertSame(requestTracker.requests[1].data, requestTracker.requests[2].data)
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/2574 */
    @Test
    fun recomposesOnlyWhenStateChanges() {
        val key = Extras.Key(0)
        val value = AtomicInteger()
        val compositionCount = AtomicInteger()
        val maxCompositionCount = AtomicInteger(3)

        composeTestRule.setContent {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("https://example.com/image")
                    .apply { extras[key] = value.getAndSet(1) }
                    .build(),
                imageLoader = imageLoader,
            )

            Image(
                painter = painter,
                contentDescription = null,
            )

            val state by painter.state.collectAsState()

            val compositions = compositionCount.incrementAndGet()
            if (compositions > maxCompositionCount.get()) {
                error("too many compositions")
            }
            if (state is State.Success) {
                maxCompositionCount.set(compositions)
            }
        }

        waitForRequestComplete()

        assertEquals(maxCompositionCount.get(), compositionCount.get())
        assertEquals(1, requestTracker.startedRequests)
        assertEquals(1, requestTracker.finishedRequests)
    }

    @Test
    fun recomposesOnlyWhenFlowEmits_before() {
        val key = Extras.Key(0)
        val value = AtomicInteger()
        val totalCompositions = 10
        val compositionCount = AtomicInteger()

        var flowEmissions = 0
        val flow = flow {
            while (flowEmissions < totalCompositions) {
                emit(flowEmissions)
                delay(20.milliseconds)
                flowEmissions++
            }
        }

        composeTestRule.setContent {
            val observableValue by flow.collectAsState(0)

            if (compositionCount.incrementAndGet() > totalCompositions) {
                error("too many compositions")
            }

            // Need to observe the value.
            observableValue.toString()

            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://example.com/image")
                        .apply { extras[key] = value.getAndIncrement() }
                        .build(),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
            )
        }

        waitForRequestComplete()

        composeTestRule.waitUntil(10_000) {
            flowEmissions >= totalCompositions
        }

        assertEquals(totalCompositions, compositionCount.get())
        assertEquals(1, requestTracker.startedRequests)
        assertEquals(1, requestTracker.finishedRequests)
    }

    @Test
    fun recomposesOnlyWhenFlowEmits_after() {
        val key = Extras.Key(0)
        val value = AtomicInteger()
        val totalCompositions = 10
        val compositionCount = AtomicInteger()

        var flowEmissions = 0
        val flow = flow {
            while (flowEmissions < totalCompositions) {
                emit(flowEmissions)
                delay(20.milliseconds)
                flowEmissions++
            }
        }

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data("https://example.com/image")
                        .apply { extras[key] = value.getAndIncrement() }
                        .build(),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
            )

            val observableValue by flow.collectAsState(0)

            if (compositionCount.incrementAndGet() > totalCompositions) {
                error("too many compositions")
            }

            // Need to observe the value.
            observableValue.toString()
        }

        waitForRequestComplete()

        composeTestRule.waitUntil(10_000) {
            flowEmissions >= totalCompositions
        }

        assertEquals(totalCompositions, compositionCount.get())
        assertEquals(1, requestTracker.startedRequests)
        assertEquals(1, requestTracker.finishedRequests)
    }

    private fun waitForRequestComplete(finishedRequests: Int = 1) {
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(10_000) {
            requestTracker.finishedRequests >= finishedRequests
        }
        composeTestRule.waitForIdle()
    }

    private fun assertLoadedBitmapSize(width: Dp, height: Dp, requestNumber: Int = 0) {
        val result = assertIs<SuccessResult>(requestTracker.results[requestNumber])
        val bitmap = assertIs<BitmapImage>(result.image).bitmap
        assertContains((width.toPx() - 1)..(width.toPx() + 1), bitmap.width)
        assertContains((height.toPx() - 1)..(height.toPx() + 1), bitmap.height)
    }

    private fun Dp.toPx() = with(composeTestRule.density) { toPx().toInt() }

    private fun Int.toDp() = with(composeTestRule.density) { toDp() }

    private val displaySize: IntSize
        get() = composeTestRule.activity.findViewById<View>(android.R.id.content)!!
            .run { IntSize(width, height) }

    companion object {
        private const val Image = "image"
    }
}
