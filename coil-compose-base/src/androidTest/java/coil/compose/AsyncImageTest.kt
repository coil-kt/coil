package coil.compose

import android.os.Build.VERSION.SDK_INT
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.base.test.R
import coil.compose.utils.ImageLoaderIdlingResource
import coil.compose.utils.ImageMockWebServer
import coil.compose.utils.assertIsSimilarTo
import coil.request.CachePolicy
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
        val expectedWidthPx = displayMetrics.widthPixels.coerceAtMost(1024)
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
        val expectedWidthPx = (displayMetrics.widthPixels / 2).coerceAtMost(1024)
        val expectedHeightPx = expectedWidthPx * 1326 / 1024

        composeTestRule.onNodeWithTag(Image)
            .assertIsDisplayed()
            .assertWidthIsEqualTo((expectedWidthPx / displayMetrics.density).dp)
            .assertHeightIsEqualTo((expectedHeightPx / displayMetrics.density).dp)
            .captureToImage()
            .assertIsSimilarTo(R.drawable.sample)
    }

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
