package coil.test

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
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
        val result = imageLoader.execute(request)
        assertIs<SuccessResult>(result)
        assertSame(drawable, result.drawable)
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
        val result = imageLoader.execute(request)
        assertIs<SuccessResult>(result)
        assertSame(drawable, result.drawable)
    }
}
