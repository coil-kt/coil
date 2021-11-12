package coil.compose

import android.os.Build.VERSION.SDK_INT
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.base.test.R
import coil.compose.utils.ImageLoaderIdlingResource
import coil.compose.utils.ImageMockWebServer
import coil.compose.utils.assertHeightIsEqualTo
import coil.compose.utils.assertIsSimilarTo
import coil.compose.utils.assertWidthIsEqualTo
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.Options
import kotlinx.coroutines.delay
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            AsyncImage(
                data = server.url("/image"),
                contentDescription = null,
                imageLoader = imageLoader,
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
    fun dynamicSize() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            AsyncImage(
                data = server.url("/image"),
                contentDescription = null,
                imageLoader = imageLoader,
                modifier = Modifier
                    .testTag(Image),
            )
        }

        waitForRequestComplete()

        val displayMetrics = composeTestRule.activity.resources.displayMetrics
        val expectedWidthPx = displayMetrics.widthPixels.toDouble().coerceAtMost(1024.0)
        val expectedHeightPx = expectedWidthPx * 1326 / 1024

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo((expectedWidthPx / displayMetrics.density).dp)
            .assertHeightIsEqualTo((expectedHeightPx / displayMetrics.density).dp)
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun dynamicHeight() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            LazyColumn(
                content = {
                    item {
                        AsyncImage(
                            data = server.url("/image"),
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

        val displayMetrics = composeTestRule.activity.resources.displayMetrics
        val expectedWidthPx = (displayMetrics.widthPixels / 2.0).coerceAtMost(1024.0)
        val expectedHeightPx = expectedWidthPx * 1326 / 1024

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo((expectedWidthPx / displayMetrics.density).dp, tolerance = 1.dp)
            .assertHeightIsEqualTo((expectedHeightPx / displayMetrics.density).dp, tolerance = 1.dp)
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

    @Test
    fun overwriteLoading() {
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            AsyncImage(
                request = ImageRequest.Builder(LocalContext.current)
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
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            AsyncImage(
                request = ImageRequest.Builder(LocalContext.current)
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
        // captureToImage is SDK_INT >= 26.
        assumeTrue(SDK_INT >= 26)

        composeTestRule.setContent {
            AsyncImage(
                data = server.url("/image"),
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

    private fun waitForRequestComplete(requestNumber: Int = 1) {
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(10_000) {
            requestTracker.finishedRequests >= requestNumber
        }
        composeTestRule.waitForIdle()
    }

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
    }
}
