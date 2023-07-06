package coil.test

import coil.ImageLoader
import coil.decode.DataSource
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.test.FakeImageLoaderEngine.OptionalInterceptor
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest

class FakeImageLoaderEngineTest {

    @Test
    fun extraData() = runTest {
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
            assertSame(drawable, result.image)
        }
    }

    @Test
    fun predicateData() = runTest {
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
            assertSame(drawable, result.image)
        }
    }

    @Test
    fun defaultDrawable() = runTest {
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
            assertSame(drawable, result.image)
        }
    }

    @Test
    fun optionalInterceptor() = runTest {
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
            assertSame(drawable, result.image)
        }
    }
}
