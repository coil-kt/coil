package coil.compose

import android.view.View
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.EventListener
import coil.ImageLoader
import coil.compose.AsyncImagePainter.State
import coil.compose.base.test.R
import coil.compose.utils.ImageLoaderIdlingResource
import coil.compose.utils.ImageMockWebServer
import coil.compose.utils.assertIsSimilarTo
import coil.compose.utils.assumeSupportsCaptureToImage
import coil.compose.utils.resourceUri
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AsyncImagePainterTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var server: MockWebServer
    private lateinit var requestTracker: ImageLoaderIdlingResource
    private lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        server = ImageMockWebServer()
        requestTracker = ImageLoaderIdlingResource()
        imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .networkObserverEnabled(false)
            .eventListener(requestTracker)
            .build()
        composeTestRule.registerIdlingResource(requestTracker)
        server.start()
    }

    @After
    fun after() {
        composeTestRule.unregisterIdlingResource(requestTracker)
        server.shutdown()
    }

    @Test
    fun basicLoad_http() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = server.url("/image"),
                    imageLoader = imageLoader
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
                    imageLoader = imageLoader
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
    fun basicLoad_drawableUri() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = resourceUri(R.drawable.sample),
                    imageLoader = imageLoader
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
        val eventListener = object : EventListener {
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
                    model = server.url("/image"),
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

        var data by mutableStateOf(server.url("/image"))

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = data,
                    imageLoader = imageLoader
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
            .assertIsSimilarTo(R.drawable.sample, threshold = 0.8)

        // Now switch the data URI to the blue drawable.
        data = server.url("/blue")

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
                model = server.url("/image"),
                imageLoader = imageLoader
            )

            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .testTag(Image)
            )

            LaunchedEffect(painter) {
                snapshotFlow { painter.state }
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
                    model = server.url("/image"),
                    imageLoader = imageLoader
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
                    .size(240.dp, 200.dp)
            ) {
                item {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = server.url("/image"),
                            imageLoader = imageLoader
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
                        .data(server.url("/noimage"))
                        .error(R.drawable.red_rectangle)
                        .build(),
                    imageLoader = imageLoader
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

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(server.url("/image"))
                            .placeholder(R.drawable.red_rectangle)
                            .build(),
                        imageLoader = imageLoader
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(128.dp)
                        .testTag(Image),
                )
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
                    model = server.url("/noimage"),
                    imageLoader = imageLoader
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
                    imageLoader = imageLoader
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
                    imageLoader = imageLoader
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
                        .data(server.url("/image"))
                        .placeholder(R.drawable.red_rectangle)
                        .crossfade(true)
                        .build(),
                    imageLoader = imageLoader
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
            .assertIsSimilarTo(R.drawable.sample, threshold = 0.85)
    }

    @Test
    fun fillMaxWidth() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            Image(
                painter = rememberAsyncImagePainter(
                    model = server.url("/image"),
                    imageLoader = imageLoader
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
                        .verticalScroll(rememberScrollState())
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = server.url("/image"),
                            imageLoader = imageLoader
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
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
                        .data(server.url("/image"))
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

    private fun waitForRequestComplete(finishedRequests: Int = 1) {
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(10_000) {
            requestTracker.finishedRequests >= finishedRequests
        }
        composeTestRule.waitForIdle()
    }

    private fun assertLoadedBitmapSize(width: Dp, height: Dp, requestNumber: Int = 0) {
        val bitmap = (requestTracker.results[requestNumber] as SuccessResult).drawable.toBitmap()
        assertEquals(bitmap.width, width.toPx())
        assertEquals(bitmap.height, height.toPx())
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
