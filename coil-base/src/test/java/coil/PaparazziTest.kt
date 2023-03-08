package coil

import android.widget.ImageView
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_6
import app.cash.paparazzi.Paparazzi
import coil.request.ImageRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = PIXEL_6,
        showSystemUi = false,
    )
    lateinit var imageLoader: ImageLoader

    @Before
    fun before() {
        imageLoader = ImageLoader(paparazzi.context)
    }

    @After
    fun after() {
        imageLoader.shutdown()
    }

    @Test
    fun loadView() {
        val imageView = ImageView(paparazzi.context)
        val request = ImageRequest.Builder(paparazzi.context)
            .data("https://example.com/image.jpg")
            .target(imageView)
            .build()
        imageLoader.enqueue(request)
        paparazzi.snapshot(imageView)
    }
}
