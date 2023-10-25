package coil.test

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.test.FakeImageLoaderEngine.OptionalInterceptor
import coil.transition.Transition
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FakeImageLoaderEngineTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `exact data`() = runTest {
        val url = "https://www.example.com/image.jpg"
        val drawable = ColorDrawable(Color.RED)
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, drawable)
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .components { add(engine) }
            .build()
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        repeat(3) {
            val result = imageLoader.execute(request)
            assertIs<SuccessResult>(result)
            assertSame(drawable, result.drawable)
        }
    }

    @Test
    fun `predicate data`() = runTest {
        val url = "https://www.example.com/image.jpg"
        val drawable = ColorDrawable(Color.RED)
        val engine = FakeImageLoaderEngine.Builder()
            .intercept("different_string", ColorDrawable(Color.GREEN))
            .intercept({ it is String && it == url }, drawable)
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .components { add(engine) }
            .build()
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        repeat(3) {
            val result = imageLoader.execute(request)
            assertIs<SuccessResult>(result)
            assertSame(drawable, result.drawable)
        }
    }

    @Test
    fun `default drawable`() = runTest {
        val url = "https://www.example.com/image.jpg"
        val drawable = ColorDrawable(Color.RED)
        val engine = FakeImageLoaderEngine.Builder()
            .intercept("different_string", ColorDrawable(Color.GREEN))
            .default(drawable)
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .components { add(engine) }
            .build()
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        repeat(3) {
            val result = imageLoader.execute(request)
            assertIs<SuccessResult>(result)
            assertSame(drawable, result.drawable)
        }
    }

    @Test
    fun `optional interceptor`() = runTest {
        var currentIndex = -1
        val url = "https://www.example.com/image.jpg"
        val drawables = listOf(
            ColorDrawable(Color.RED),
            ColorDrawable(Color.GREEN),
            ColorDrawable(Color.BLUE),
        )
        fun testInterceptor(index: Int) = OptionalInterceptor { chain ->
            if (currentIndex == index) {
                SuccessResult(drawables[index], chain.request, DataSource.MEMORY)
            } else {
                null
            }
        }
        val engine = FakeImageLoaderEngine.Builder()
            .addInterceptor {
                currentIndex++
                null
            }
            .addInterceptor(testInterceptor(0))
            .addInterceptor(testInterceptor(1))
            .addInterceptor(testInterceptor(2))
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .components { add(engine) }
            .build()
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        for (drawable in drawables) {
            val result = imageLoader.execute(request)
            assertIs<SuccessResult>(result)
            assertSame(drawable, result.drawable)
        }
    }

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

    @Test
    fun `request transformer enforces invariants`() = runTest {
        val url = "https://www.example.com/image.jpg"
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, ColorDrawable(Color.RED))
            .requestTransformer { request ->
                request.newBuilder()
                    .data(null)
                    .build()
            }
            .build()
        val imageLoader = ImageLoader.Builder(context)
            .components { add(engine) }
            .build()
        val request = ImageRequest.Builder(context)
            .data(url)
            .build()

        val result = imageLoader.execute(request)
        assertIs<ErrorResult>(result)
        assertIs<IllegalStateException>(result.throwable)
    }
}
