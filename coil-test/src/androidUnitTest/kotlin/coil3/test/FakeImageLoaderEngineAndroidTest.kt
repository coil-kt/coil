package coil3.test

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.request.transitionFactory
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import coil3.transition.Transition
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest

class FakeImageLoaderEngineAndroidTest : RobolectricTest() {

    @Test
    fun `removes transition factory`() = runTest {
        val url = "https://www.example.com/image.jpg"
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, ColorDrawable(Color.RED))
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .components { add(engine) }
            .build()
        val request = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build()

        val result = imageLoader.execute(request)
        assertIs<SuccessResult>(result)
        assertSame(Transition.Factory.NONE, result.request.transitionFactory)
    }
}
