package coil3.transform

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import coil3.ImageLoader
import coil3.decode.GifDecoder
import coil3.decode.ImageDecoderDecoder
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.onAnimationEnd
import coil3.request.onAnimationStart
import coil3.request.repeatCount
import coil3.request.target
import coil3.util.ViewTestActivity
import coil3.util.activity
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AnimationCallbacksTest {

    private lateinit var context: Context
    private lateinit var imageLoader: ImageLoader

    @get:Rule
    val activityRule = activityScenarioRule<ViewTestActivity>()

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext() as Context
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
    fun callbacksTest() = runTest(timeout = 30.seconds) {
        val imageView = activityRule.scenario.activity.imageView
        val isStartCalled = MutableStateFlow(false)
        val isEndCalled = MutableStateFlow(false)
        val decoderFactory = if (SDK_INT >= 28) {
            ImageDecoderDecoder.Factory()
        } else {
            GifDecoder.Factory()
        }

        val request = ImageRequest.Builder(context)
            .data("$SCHEME_FILE:///android_asset/animated.gif")
            .target(imageView)
            .decoderFactory(decoderFactory)
            .repeatCount(0)
            .onAnimationStart {
                isStartCalled.value = true
            }
            .onAnimationEnd {
                isEndCalled.value = true
            }
            .build()
        val result = imageLoader.execute(request)
        if (result is ErrorResult) throw result.throwable
        isStartCalled.first { it }
        isEndCalled.first { it }
    }
}
