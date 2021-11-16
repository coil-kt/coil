package coil.compose

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.base.test.R
import coil.compose.utils.ImageLoaderIdlingResource
import coil.compose.utils.ImageMockWebServer
import coil.compose.utils.assertHeightIsEqualTo
import coil.compose.utils.assertIsSimilarTo
import coil.compose.utils.assertWidthIsEqualTo
import coil.compose.utils.assumeSupportsCaptureToImage
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.Options
import coil.request.SuccessResult
import coil.size.PixelSize
import kotlinx.coroutines.delay
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AsyncImageTest {

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
    fun fixedSize() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            AsyncImage(
                model = server.url("/image"),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .size(128.dp, 166.dp)
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        assertLoadedBitmapSize(128.dp.toPx(), 166.dp.toPx())

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(128.dp)
            .assertHeightIsEqualTo(166.dp)
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun fillMaxWidth() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            AsyncImage(
                model = server.url("/image"),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        val expectedWidthPx = displaySize.width.toDouble()
        val expectedHeightPx = expectedWidthPx * SampleHeight / SampleWidth

        assertLoadedBitmapSize(
            width = expectedWidthPx.toInt().coerceAtMost(SampleWidth),
            height = expectedHeightPx.toInt().coerceAtMost(SampleHeight)
        )

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun fillMaxHeight() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            AsyncImage(
                model = server.url("/image"),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .fillMaxHeight()
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        val expectedHeightPx = displaySize.height.toDouble()
        val expectedWidthPx = (expectedHeightPx * SampleWidth / SampleHeight)
            .coerceAtMost(displaySize.width.toDouble())

        assertLoadedBitmapSize(
            width = expectedWidthPx.toInt().coerceAtMost(SampleWidth),
            height = expectedHeightPx.toInt().coerceAtMost(SampleHeight)
        )

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun dynamicSize() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            AsyncImage(
                model = server.url("/image"),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        val expectedWidthPx = displaySize.width.toDouble().coerceAtMost(SampleWidth.toDouble())
        val expectedHeightPx = expectedWidthPx * SampleHeight / SampleWidth

        assertLoadedBitmapSize(
            width = expectedWidthPx.toInt().coerceAtMost(SampleWidth),
            height = expectedHeightPx.toInt().coerceAtMost(SampleHeight)
        )

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun dynamicHeight() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            LazyColumn(
                content = {
                    item {
                        AsyncImage(
                            model = server.url("/image"),
                            contentDescription = null,
                            imageLoader = imageLoader,
                            modifier = Modifier.testTag(Image),
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.5f)
            )
        }

        waitForRequestComplete()

        val expectedWidthPx = (displaySize.width / 2.0).coerceAtMost(SampleWidth.toDouble())
        val expectedHeightPx = expectedWidthPx * SampleHeight / SampleWidth

        assertLoadedBitmapSize(
            width = expectedWidthPx.toInt().coerceAtMost(SampleWidth),
            height = expectedHeightPx.toInt().coerceAtMost(SampleHeight)
        )

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(expectedWidthPx.toDp(), tolerance = 1.dp)
            .assertHeightIsEqualTo(expectedHeightPx.toDp(), tolerance = 1.dp)
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun overwriteLoading() {
        assumeSupportsCaptureToImage()

        // Remove this or `setContent` will timeout.
        composeTestRule.unregisterIdlingResource(requestTracker)

        composeTestRule.setContent {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(server.url("/image"))
                    .fetcherFactory(LoadingFetcher.Factory())
                    .build(),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .testTag(Image),
                loading = {
                    Box(
                        modifier = Modifier
                            .background(color = Color.Blue)
                    )
                }
            )

            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .background(color = Color.Blue)
                    .testTag(Loading),
            )
        }

        composeTestRule.waitUntil(10_000) {
            requestTracker.startedRequests >= 1
        }

        val expected = composeTestRule.onNodeWithTag(Loading).captureToImage()
        val actual = composeTestRule.onNodeWithTag(Image).captureToImage()
        expected.assertIsSimilarTo(actual)
    }

    @Test
    fun overwriteError() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(server.url("/image"))
                    .fetcherFactory(ErrorFetcher.Factory())
                    .build(),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .testTag(Image),
                error = {
                    Box(
                        modifier = Modifier
                            .background(color = Color.Blue)
                    )
                }
            )

            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .background(color = Color.Blue)
                    .testTag(Error),
            )
        }

        waitForRequestComplete()

        val expected = composeTestRule.onNodeWithTag(Error).captureToImage()
        val actual = composeTestRule.onNodeWithTag(Image).captureToImage()
        expected.assertIsSimilarTo(actual)
    }

    @Test
    fun overwriteSuccess() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            AsyncImage(
                model = server.url("/image"),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .testTag(Image),
                success = {
                    Box(
                        modifier = Modifier
                            .background(color = Color.Blue)
                    )
                }
            )

            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .size(128.dp, 128.dp)
                    .background(color = Color.Blue)
                    .testTag(Success),
            )
        }

        waitForRequestComplete()

        val expected = composeTestRule.onNodeWithTag(Success).captureToImage()
        val actual = composeTestRule.onNodeWithTag(Image).captureToImage()
        expected.assertIsSimilarTo(actual)
    }

    @Test
    fun listener() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(server.url("/image"))
                    // Ensure this doesn't constantly recompose or restart image requests.
                    .listener { _, _ -> }
                    .build(),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        val expectedWidthPx = displaySize.width.toDouble().coerceAtMost(SampleWidth.toDouble())
        val expectedHeightPx = expectedWidthPx * SampleHeight / SampleWidth

        assertLoadedBitmapSize(
            width = expectedWidthPx.toInt().coerceAtMost(SampleWidth),
            height = expectedHeightPx.toInt().coerceAtMost(SampleHeight)
        )

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    private fun waitForRequestComplete(finishedRequests: Int = 1) {
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(10_000) {
            requestTracker.finishedRequests >= finishedRequests
        }
        composeTestRule.waitForIdle()
    }

    private fun assertLoadedBitmapSize(width: Int, height: Int, requestNumber: Int = 0) {
        val bitmap = (requestTracker.results[requestNumber] as SuccessResult).drawable.toBitmap()
        assertEquals(bitmap.width, width)
        assertEquals(bitmap.height, height)
    }

    private fun Dp.toPx() = with(composeTestRule.density) { toPx().toInt() }

    private fun Double.toDp() = with(composeTestRule.density) { toInt().toDp() }

    private val displaySize: PixelSize
        get() = composeTestRule.activity.findViewById<View>(android.R.id.content)!!
            .run { PixelSize(width, height) }

    private class LoadingFetcher : Fetcher {

        override suspend fun fetch(): FetchResult? {
            while (true) delay(100)
        }

        class Factory : Fetcher.Factory<Any> {
            override fun create(data: Any, options: Options, imageLoader: ImageLoader) = LoadingFetcher()
        }
    }

    private class ErrorFetcher : Fetcher {

        override suspend fun fetch(): FetchResult? {
            throw IllegalStateException()
        }

        class Factory : Fetcher.Factory<Any> {
            override fun create(data: Any, options: Options, imageLoader: ImageLoader) = ErrorFetcher()
        }
    }

    companion object {
        private const val Image = "image"
        private const val Loading = "loading"
        private const val Error = "error"
        private const val Success = "success"
        private const val SampleWidth = 1024
        private const val SampleHeight = 1326
    }
}
