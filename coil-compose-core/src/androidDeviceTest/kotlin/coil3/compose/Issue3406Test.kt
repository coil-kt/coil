package coil3.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.rules.activityScenarioRule
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.test.utils.ComposeTestActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test

class Issue3406Test {

    @get:Rule
    val activityRule = activityScenarioRule<ComposeTestActivity>()

    private var imageLoader: ImageLoader? = null

    @After
    fun after() {
        imageLoader?.shutdown()
    }

    @Test
    fun unsplashProfileImage64LoadsWithAsyncImage() {
        val result = AtomicReference<ImageResult?>()
        val latch = CountDownLatch(1)

        activityRule.scenario.onActivity { activity ->
            imageLoader = ImageLoader.Builder(activity)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .components {
                    add(OkHttpNetworkFetcherFactory())
                }
                .build()

            activity.setContentView(
                ComposeView(activity).apply {
                    setContent {
                        AsyncImage(
                            model = UNSPLASH_PROFILE_IMAGE_64,
                            contentDescription = "Avatar",
                            imageLoader = imageLoader!!,
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.Gray),
                            contentScale = ContentScale.Crop,
                            onState = { state ->
                                when (state) {
                                    is AsyncImagePainter.State.Error -> {
                                        result.set(state.result)
                                        latch.countDown()
                                    }
                                    is AsyncImagePainter.State.Success -> {
                                        result.set(state.result)
                                        latch.countDown()
                                    }
                                    else -> {}
                                }
                            },
                        )
                    }
                },
            )
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS))
        val imageResult = result.get()
        if (imageResult is ErrorResult) {
            throw imageResult.throwable
        } else {
            assertIs<SuccessResult>(imageResult)
        }
    }

    companion object {
        private const val UNSPLASH_PROFILE_IMAGE_64 =
            "https://images.unsplash.com/profile-1773680726147-06990da7c26fimage" +
                "?ixlib=rb-4.1.0&crop=faces&fit=crop&w=64&h=64"
    }
}
