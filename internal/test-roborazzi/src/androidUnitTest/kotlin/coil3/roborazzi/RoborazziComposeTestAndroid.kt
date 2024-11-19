package coil3.roborazzi

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.test.ColorImage
import coil3.test.ColorImage.Companion.Red
import coil3.test.FakeImageLoaderEngine
import coil3.test.utils.ComposeTestActivity
import coil3.test.utils.RobolectricTest
import com.github.takahirom.roborazzi.RoborazziRule
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoborazziComposeTestAndroid : RobolectricTest() {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComposeTestActivity>()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
        options = RoborazziRule.Options(
            captureType = RoborazziRule.CaptureType.LastImage(),
            outputDirectoryPath = "src/androidUnitTest/snapshots/images",
        )
    )

    @Test
    fun asyncImage() {
        val url = "https://www.example.com/image.jpg"
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, ColorImage(Red, width = 100, height = 100))
            .build()
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()

        composeTestRule.setContent {
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
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, ColorImage(Red, width = 100, height = 100))
            .build()
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()

        composeTestRule.setContent {
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
    }
}
