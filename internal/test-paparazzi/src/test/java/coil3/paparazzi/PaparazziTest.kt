package coil3.paparazzi

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.decode.ImageSource
import coil3.request.ImageRequest
import coil3.request.target
import coil3.size.Size
import coil3.test.FakeImageLoaderEngine
import coil3.test.intercept
import kotlin.test.assertTrue
import okio.Buffer
import okio.FileSystem
import org.junit.Rule
import org.junit.Test

class PaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 320,
            screenHeight = 470,
        ),
        theme = "android:Theme.Material.Light.NoActionBar.Fullscreen",
        showSystemUi = false,
    )

    @Test
    fun imageView() {
        val url = "https://www.example.com/image.jpg"
        val drawable = object : ColorDrawable(Color.RED) {
            override fun getIntrinsicWidth() = 100
            override fun getIntrinsicHeight() = 100
        }
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, drawable)
            .build()
        val imageLoader = ImageLoader.Builder(paparazzi.context)
            .components { add(engine) }
            .build()
        val imageView = ImageView(paparazzi.context)
        imageView.scaleType = ImageView.ScaleType.CENTER
        val request = ImageRequest.Builder(paparazzi.context)
            .data(url)
            .target(imageView)
            .build()

        // Don't suspend to test that the image view is updated synchronously.
        imageLoader.enqueue(request)

        paparazzi.snapshot(imageView)
    }

    @Test
    fun asyncImage() {
        val url = "https://www.example.com/image.jpg"
        val drawable = object : ColorDrawable(Color.RED) {
            override fun getIntrinsicWidth() = 100
            override fun getIntrinsicHeight() = 100
        }
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, drawable)
            .build()
        val imageLoader = ImageLoader.Builder(paparazzi.context)
            .components { add(engine) }
            .build()

        paparazzi.snapshot {
            AsyncImage(
                model = url,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.None,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    @Test
    fun rememberAsyncImagePainter() {
        val url = "https://www.example.com/image.jpg"
        val drawable = object : ColorDrawable(Color.RED) {
            override fun getIntrinsicWidth() = 100
            override fun getIntrinsicHeight() = 100
        }
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, drawable)
            .build()
        val imageLoader = ImageLoader.Builder(paparazzi.context)
            .components { add(engine) }
            .build()

        paparazzi.snapshot {
            Image(
                painter = rememberAsyncImagePainter(
                    // TODO: Figure out how to avoid having to specify an immediate size.
                    model = ImageRequest.Builder(paparazzi.context)
                        .data(url)
                        .size(Size.ORIGINAL)
                        .build(),
                    imageLoader = imageLoader,
                ),
                contentDescription = null,
                contentScale = ContentScale.None,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    /** Regression test: https://github.com/coil-kt/coil/issues/1754 */
    @Test
    fun createSourceImageSource() {
        val source = ImageSource(Buffer(), FileSystem.SYSTEM)
        assertTrue(source.source().exhausted())
    }
}
