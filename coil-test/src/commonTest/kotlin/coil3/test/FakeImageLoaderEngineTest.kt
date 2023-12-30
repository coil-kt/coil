package coil3.test

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.test.FakeImageLoaderEngine.OptionalInterceptor
import coil3.test.utils.FakeImage
import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.test.runTest

class FakeImageLoaderEngineTest : RobolectricTest() {

    @Test
    fun extraData() = runTest {
        val url = "https://www.example.com/image.jpg"
        val image = FakeImage()
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, image)
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
            assertSame(image, result.image)
        }
    }

    @Test
    fun predicateData() = runTest {
        val url = "https://www.example.com/image.jpg"
        val image = FakeImage()
        val engine = FakeImageLoaderEngine.Builder()
            .intercept("different_string", FakeImage())
            .intercept({ it is String && it == url }, image)
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
            assertSame(image, result.image)
        }
    }

    @Test
    fun defaultDrawable() = runTest {
        val url = "https://www.example.com/image.jpg"
        val image = FakeImage()
        val engine = FakeImageLoaderEngine.Builder()
            .intercept("different_string", FakeImage())
            .default(image)
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
            assertSame(image, result.image)
        }
    }

    @Test
    fun optionalInterceptor() = runTest {
        var currentIndex = -1
        val url = "https://www.example.com/image.jpg"
        val images = listOf(FakeImage(), FakeImage(), FakeImage())
        fun testInterceptor(index: Int) = OptionalInterceptor { chain ->
            if (currentIndex == index) {
                SuccessResult(images[index], chain.request, DataSource.MEMORY)
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

        for (drawable in images) {
            val result = imageLoader.execute(request)
            assertIs<SuccessResult>(result)
            assertSame(drawable, result.image)
        }
    }

    @Test
    fun requestTransformerEnforcesInvariants() = runTest {
        val url = "https://www.example.com/image.jpg"
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, FakeImage())
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
