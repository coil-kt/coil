package coil3.roborazzi

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.test.FakeImageLoaderEngine
import coil3.test.intercept
import coil3.test.utils.ComposeTestActivity
import com.github.takahirom.roborazzi.RoborazziRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoborazziComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComposeTestActivity>()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeTestRule,
        captureRoot = composeTestRule.onRoot(),
        options = RoborazziRule.Options(
            captureType = RoborazziRule.CaptureType.LastImage(),
            outputDirectoryPath = "src/test/snapshots/images",
        )
    )

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
        val drawable = object : ColorDrawable(Color.RED) {
            override fun getIntrinsicWidth() = 100
            override fun getIntrinsicHeight() = 100
        }
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, drawable)
            .build()
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()

        composeTestRule.setContent {
            Image(
                // TODO: Figure out how to avoid having to specify an immediate size.
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(composeTestRule.activity)
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
}
