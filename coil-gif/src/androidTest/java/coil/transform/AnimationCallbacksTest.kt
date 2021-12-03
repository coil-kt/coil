package coil.transform

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.activityScenarioRule
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.onAnimationEnd
import coil.request.onAnimationStart
import coil.request.repeatCount
import coil.util.TestActivity
import coil.util.activity
import coil.util.runTestMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun callbacksTest() = runTestMain {
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
        withTimeout(30_000) { isStartCalled.first { it } }
        withTimeout(30_000) { isEndCalled.first { it } }
    }
}
