package coil.test

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.widget.ImageView
import androidx.compose.ui.layout.ContentScale
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_6
import app.cash.paparazzi.Paparazzi
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.junit.Rule
import org.junit.Test

class PaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar.Fullscreen",
        showSystemUi = false,
    )

    @Test
    fun loadView() {
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
    fun loadCompose() {
        val url = "https://www.example.com/image.jpg"
        // Wrap the color drawable so it isn't automatically converted into a ColorPainter.
        val drawable = object : LayerDrawable(arrayOf(ColorDrawable(Color.RED))) {
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
            )
        }
    }
}
