package coil3.roborazzi

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.test.FakeImage
import coil3.test.FakeImageLoaderEngine
import coil3.toBitmap
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

class RoborazziComposeTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun asyncImageJvm() {
        val url = "https://www.example.com/image.jpg"
        val image = FakeImage(
            width = 100,
            height = 100,
            color = 0xFFFF0000.toInt()
        )

        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, image.toBitmap().asImage())
            .build()

        val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE)
            .components { add(engine) }
            .build()

        runDesktopComposeUiTest {
            setContent {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    imageLoader = imageLoader,
                    contentScale = ContentScale.None,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            onRoot().captureRoboImage()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rememberAsyncImagePainterJvm() {
        val url = "https://www.example.com/image.jpg"
        val image = FakeImage(
            width = 100,
            height = 100,
            color = 0xFFFF0000.toInt()
        )
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, image.toBitmap().asImage())
            .build()
        val imageLoader = ImageLoader.Builder(PlatformContext.INSTANCE)
            .components { add(engine) }
            .build()

        runDesktopComposeUiTest {
            setContent {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = url,
                        imageLoader = imageLoader,
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.None,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            onRoot().captureRoboImage()
        }
    }
}
