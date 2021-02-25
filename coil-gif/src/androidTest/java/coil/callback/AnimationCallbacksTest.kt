package coil.callback

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.onAnimationEnd
import coil.request.onAnimationStart
import coil.request.repeatCount
import coil.util.TestActivity
import coil.util.activity
import coil.util.runBlockingTest
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class AnimationCallbacksTest {

    private lateinit var context: Context
    private lateinit var imageLoader: ImageLoader

    @get:Rule
    val activityRule = activityScenarioRule<TestActivity>()

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        imageLoader = ImageLoader.Builder(context)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
        activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun callbacksTest() = runBlockingTest {
        val imageView = activityRule.scenario.activity.imageView
        var isStartCalled = false
        var isEndCalled = false
        val decoder = if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoderDecoder()
        } else {
            GifDecoder()
        }

        val imageRequest = ImageRequest.Builder(context)
            .repeatCount(0)
            .onAnimationStart {
                isStartCalled = true
            }
            .onAnimationEnd {
                isEndCalled = true
            }
            .target(imageView)
            .decoder(decoder)
            .data("${ContentResolver.SCHEME_FILE}:///android_asset/animated.gif")
            .build()
        imageLoader.enqueue(imageRequest)
        delay(5000)
        assertTrue(isStartCalled)
        assertTrue(isEndCalled)
    }
}
