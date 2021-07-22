package coil.compose

import android.os.Build.VERSION.SDK_INT
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
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
import androidx.compose.ui.unit.dp
import coil.EventListener
import coil.ImageLoader
import coil.compose.ImagePainter.State
import coil.compose.base.test.R
import coil.compose.utils.ImageLoaderIdlingResource
import coil.compose.utils.ImageMockWebServer
import coil.compose.utils.assertIsSimilarTo
import coil.compose.utils.resourceUri
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ImagePainterTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var server: MockWebServer
    private lateinit var requestTracker: ImageLoaderIdlingResource
    private lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        server = ImageMockWebServer()
        requestTracker = ImageLoaderIdlingResource()
        val context = composeTestRule.activity.applicationContext
        imageLoader = ImageLoader.Builder(context)
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
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(server.url("/image")),
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
    fun basicLoad_drawableId() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(R.drawable.sample),
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
    fun basicLoad_drawableUri() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(resourceUri(R.drawable.sample)),
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
                painter = rememberImagePainter(
                    data = server.url("/image"),
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
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        var data by mutableStateOf(server.url("/image"))

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(data),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete(requestNumber = 1)

        // Assert that the content is completely red.
        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample, threshold = 0.8)

        // Now switch the data URI to the blue drawable.
        data = server.url("/blue")

        waitForRequestComplete(requestNumber = 2)

        // Assert that the content is completely blue.
        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertIsSimilarTo(R.drawable.blue_rectangle)
    }

    @Test
    fun basicLoad_changeSize() {
        val scope = TestCoroutineScope()
        scope.launch {
            val states = Channel<State>()
            var size by mutableStateOf(128.dp)

            composeTestRule.setContent {
                val painter = rememberImagePainter(server.url("/image"))

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
        scope.cleanupTestCoroutines()
    }

    @Test
    fun basicLoad_nosize() {
        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(server.url("/image")),
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
    fun lazycolumn() {
        composeTestRule.setContent {
            LazyColumn(
                modifier = Modifier
                    .size(240.dp, 200.dp)
            ) {
                item {
                    Image(
                        painter = rememberImagePainter(server.url("/image")),
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
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(
                    data = server.url("/noimage"),
                    builder = { error(R.drawable.red_rectangle) }
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
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                Image(
                    painter = rememberImagePainter(
                        data = server.url("/image"),
                        builder = { placeholder(R.drawable.red_rectangle) }
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
                painter = rememberImagePainter(server.url("/noimage")),
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
                painter = rememberImagePainter(
                    painterResource(R.drawable.sample),
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
                painter = rememberImagePainter(
                    painterResource(R.drawable.black_rectangle_vector),
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
                painter = rememberImagePainter(ColorPainter(Color.Magenta)),
                contentDescription = null,
                modifier = Modifier.size(128.dp),
            )
        }
    }

    @Test
    fun crossfade() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(
                    data = server.url("/image"),
                    builder = {
                        placeholder(R.drawable.red_rectangle)
                        crossfade(true)
                    }
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
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Composable
    private inline fun rememberImagePainter(
        data: Any,
        builder: ImageRequest.Builder.() -> Unit = {}
    ) = rememberImagePainter(data, imageLoader, builder = builder)

    private fun waitForRequestComplete(requestNumber: Int = 1) {
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(10_000) {
            requestTracker.finishedRequests >= requestNumber
        }
        composeTestRule.waitForIdle()
    }

    companion object {
        private const val Image = "image"
    }
}
