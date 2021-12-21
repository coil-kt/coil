package coil.compose

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.compose.AsyncImagePainter.State
import coil.compose.base.test.R
import coil.compose.utils.ImageLoaderIdlingResource
import coil.compose.utils.ImageMockWebServer
import coil.compose.utils.assertHeightIsEqualTo
import coil.compose.utils.assertIsSimilarTo
import coil.compose.utils.assertWidthIsEqualTo
import coil.compose.utils.assumeSupportsCaptureToImage
import coil.decode.DecodeUtils
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.Options
import coil.request.SuccessResult
import coil.size.Scale
import kotlinx.coroutines.delay
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

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

        assertSampleLoadedBitmapSize(expectedWidthPx, expectedHeightPx)

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

        assertSampleLoadedBitmapSize(expectedWidthPx, expectedHeightPx)

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

        assertSampleLoadedBitmapSize(expectedWidthPx, expectedHeightPx)

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

        assertSampleLoadedBitmapSize(expectedWidthPx, expectedHeightPx)

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

        val actual = composeTestRule.onNodeWithTag(Image).captureToImage()
        val expected = composeTestRule.onNodeWithTag(Loading).captureToImage()
        actual.assertIsSimilarTo(expected)
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

        val actual = composeTestRule.onNodeWithTag(Image).captureToImage()
        val expected = composeTestRule.onNodeWithTag(Error).captureToImage()
        actual.assertIsSimilarTo(expected)
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

        val actual = composeTestRule.onNodeWithTag(Image).captureToImage()
        val expected = composeTestRule.onNodeWithTag(Success).captureToImage()
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun overwriteContent() {
        assumeSupportsCaptureToImage()

        var index = 0

        composeTestRule.setContent {
            AsyncImage(
                model = server.url("/image"),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .size(100.dp)
                    .testTag(Image)
            ) {
                val state = painter.state
                SideEffect {
                    when (index) {
                        0 -> {
                            assertIs<State.Loading>(state)
                            assertEquals(painter.state, state)
                            assertNull(state.painter)
                        }
                        1 -> {
                            assertIs<State.Success>(state)
                            assertEquals(painter.state, state)
                        }
                        else -> fail("Recomposed too many times. State: $state")
                    }
                    index++
                }
                AsyncImageContent(
                    modifier = Modifier.clip(CircleShape)
                )
            }

            Spacer(
                modifier = Modifier
                    .height(8.dp)
                    .fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .testTag(Content),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sample),
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                )
            }
        }

        waitForRequestComplete()

        val actual = composeTestRule.onNodeWithTag(Image).captureToImage()
        val expected = composeTestRule.onNodeWithTag(Content).captureToImage()
        actual.assertIsSimilarTo(expected)
    }

    @Test
    fun listener() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            val value = "" // Use a fake value inside the listener to make it stateful.
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(server.url("/image"))
                    // Ensure this doesn't constantly recompose or restart image requests.
                    .listener { _, _ -> value + "" }
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

        assertSampleLoadedBitmapSize(expectedWidthPx, expectedHeightPx)

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun fillMaxSize_scaleFill() {
        assumeSupportsCaptureToImage()

        composeTestRule.setContent {
            AsyncImage(
                model = server.url("/image"),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Image),
                contentScale = ContentScale.Crop
            )
        }

        waitForRequestComplete()

        val expectedWidthPx = displaySize.width.toDouble()
        val expectedHeightPx = displaySize.height.toDouble()

        assertSampleLoadedBitmapSize(expectedWidthPx, expectedHeightPx, scale = Scale.FILL)

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo(expectedWidthPx.toDp())
            .assertHeightIsEqualTo(expectedHeightPx.toDp())
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample, scale = Scale.FILL)
    }

    @Test
    fun doesNotRecompose() {
        val compositionCount = AtomicInteger()

        composeTestRule.setContent {
            compositionCount.getAndIncrement()
            AsyncImage(
                model = server.url("/incorrect_path"),
                contentDescription = null,
                imageLoader = imageLoader
            )
        }

        waitForRequestComplete()

        assertEquals(1, compositionCount.get())
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
        assertTrue(bitmap.width in (width - 1)..(width + 1))
        assertTrue(bitmap.height in (height - 1)..(height + 1))
    }

    private fun assertSampleLoadedBitmapSize(
        composableWidth: Double,
        composableHeight: Double,
        scale: Scale = Scale.FIT,
        requestNumber: Int = 0
    ) {
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = SampleWidth.toDouble(),
            srcHeight = SampleHeight.toDouble(),
            dstWidth = composableWidth,
            dstHeight = composableHeight,
            scale = scale
        ).coerceAtMost(1.0)
        assertLoadedBitmapSize(
            width = (multiplier * SampleWidth).toInt(),
            height = (multiplier * SampleHeight).toInt(),
            requestNumber = requestNumber
        )
    }

    private fun Dp.toPx() = with(composeTestRule.density) { toPx().toInt() }

    private fun Double.toDp() = with(composeTestRule.density) { toInt().toDp() }

    private val displaySize: IntSize
        get() = composeTestRule.activity.findViewById<View>(android.R.id.content)!!
            .run { IntSize(width, height) }

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
        private const val Content = "content"
        private const val SampleWidth = 1024
        private const val SampleHeight = 1326
    }
}
