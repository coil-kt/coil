package coil.test

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.util.ComposeTestActivity
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
            outputDirectoryPath = "src/test/snapshots/images",
        )
    )

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
        val imageLoader = ImageLoader.Builder(composeTestRule.activity)
            .components { add(engine) }
            .build()

        composeTestRule.setContent {
            AsyncImage(
                model = url,
                contentDescription = null,
                imageLoader = imageLoader,
                contentScale = ContentScale.None,
            )
        }
    }
}
