package coil.compose

import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_6
import app.cash.paparazzi.Paparazzi
import coil.ImageLoader
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PaparazziTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = PIXEL_6,
        theme = "android:Theme.Material.Light.NoActionBar.Fullscreen",
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
    fun asyncImage() {
        paparazzi.snapshot {
            AsyncImage(
                model = "https://example.com/image.jpg",
                contentDescription = null,
                imageLoader = imageLoader,
            )
        }
    }
}
