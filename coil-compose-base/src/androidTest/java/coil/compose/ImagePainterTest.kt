package coil.compose

import android.graphics.drawable.ShapeDrawable
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
import androidx.test.filters.LargeTest
import coil.EventListener
import coil.ImageLoader
import coil.compose.base.test.R
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.ImageResult
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
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@LargeTest
@RunWith(JUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ImagePainterTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var server: MockWebServer
    private lateinit var requestTracker: ImageLoaderIdlingResource
    private lateinit var imageLoader: ImageLoader

    @Before
    fun setup() {
        server = ImageMockWebServer()
        requestTracker = ImageLoaderIdlingResource()
        imageLoader = ImageLoader.Builder(composeTestRule.activity.applicationContext)
            .diskCachePolicy(CachePolicy.DISABLED)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .eventListener(requestTracker)
            .build()
        composeTestRule.registerIdlingResource(requestTracker)
        server.start()
    }

    @After
    fun teardown() {
        composeTestRule.unregisterIdlingResource(requestTracker)
        server.shutdown()
    }

    @Test
    fun basicLoad_http() {
        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(server.url("/image")),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
    }

    @Test
    fun basicLoad_drawableId() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(R.drawable.red_rectangle),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertPixels(Color.Red)
    }

    @Test
    fun basicLoad_drawableUri() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(resourceUri(R.drawable.red_rectangle)),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertPixels(Color.Red)
    }

    @Test
    fun basicLoad_customImageLoader() {
        var requestCompleted by mutableStateOf(false)

        // Build a custom ImageLoader with an EventListener
        val eventListener = object : EventListener {
            override fun onSuccess(request: ImageRequest, metadata: ImageResult.Metadata) {
                requestCompleted = true
            }
        }
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .eventListener(eventListener)
            .build()

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(
                    data = server.url("/image"),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                modifier = Modifier.size(128.dp, 128.dp),
            )
        }

        composeTestRule.waitForIdle()
        // Wait for the event listener to run
        composeTestRule.waitUntil(10_000) { requestCompleted }
    }

    @Test
    fun basicLoad_switchData() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        var data by mutableStateOf(server.url("/red"))

        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(data),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        // Assert that the content is completely Red
        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertPixels(Color.Red)

        // Now switch the data URI to the blue drawable
        data = server.url("/blue")

        // Assert that the content is completely Blue
        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertPixels(Color.Blue)
    }

    @Test
    fun basicLoad_changeSize() {
        val scope = TestCoroutineScope()
        scope.launch {
            val states = Channel<ImagePainter.State>()
            var size by mutableStateOf(128.dp)

            composeTestRule.setContent {
                val painter = rememberImagePainter(server.url("/red"))

                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size)
                        .testTag(Image),
                )

                LaunchedEffect(painter) {
                    snapshotFlow { painter.state }
                        .filter { it is ImagePainter.State.Success || it is ImagePainter.State.Error }
                        .onCompletion { states.cancel() }
                        .collect { states.send(it) }
                }
            }

            // Await the first load
            assertNotNull(states.receive())

            // Now change the size
            size = 256.dp
            composeTestRule.awaitIdle()

            // Await any potential subsequent load (which shouldn't come)
            val result = withTimeoutOrNull(3000) { states.receive() }
            assertNull(result)

            // Close the signal channel
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
    fun lazycolumn_nosize() {
        composeTestRule.setContent {
            LazyColumn {
                item {
                    Image(
                        painter = rememberImagePainter(server.url("/image")),
                        contentDescription = null,
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .testTag(Image),
                    )
                }
            }
        }

        waitForRequestComplete()

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsAtLeast(1.dp)
            .assertHeightIsAtLeast(1.dp)
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

        // Assert that the error drawable was drawn
        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            .assertPixels(Color.Red)
    }

    @Test
    fun previewPlaceholder() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                Image(
                    painter = rememberImagePainter(
                        data = "blah",
                        builder = { placeholder(R.drawable.red_rectangle) }
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(128.dp, 128.dp)
                        .testTag(Image),
                )
            }
        }

        composeTestRule.onNodeWithTag(Image)
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
            .assertIsDisplayed()
            .captureToImage()
            // We're probably scaling a bitmap up in size, so increase the tolerance to 5%
            // to not fail due to small scaling artifacts.
            .assertPixels(Color.Red, tolerance = 0.05f)
    }

    @Test
    fun errorStillHasSize() {
        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(server.url("/noimage")),
                contentDescription = null,
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        // Assert that the layout is in the tree and has the correct size
        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(128.dp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun data_drawable_throws() {
        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(ShapeDrawable()),
                contentDescription = null,
                modifier = Modifier.size(128.dp, 128.dp),
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun data_imagebitmap_throws() {
        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(
                    painterResource(android.R.drawable.ic_delete),
                ),
                contentDescription = null,
                modifier = Modifier.size(128.dp, 128.dp),
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
                modifier = Modifier.size(128.dp, 128.dp),
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun data_painter_throws() {
        composeTestRule.setContent {
            Image(
                painter = rememberImagePainter(ColorPainter(Color.Magenta)),
                contentDescription = null,
                modifier = Modifier.size(128.dp, 128.dp),
            )
        }
    }

    @Composable
    private inline fun rememberImagePainter(
        data: Any,
        builder: ImageRequest.Builder.() -> Unit = {}
    ) = rememberImagePainter(data, imageLoader, builder = builder)

    private fun waitForRequestComplete() {
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(5_000) {
            requestTracker.finishedRequests > 0
        }
        composeTestRule.waitForIdle()
    }

    companion object {
        private const val Image = "image"
    }
}
