package coil3.roborazzi

import android.widget.ImageView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.rules.activityScenarioRule
import coil3.ColorImage
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.target
import coil3.test.FakeImageLoaderEngine
import coil3.test.utils.RobolectricTest
import coil3.test.utils.ViewTestActivity
import coil3.test.utils.activity
import com.github.takahirom.roborazzi.RoborazziRule
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoborazziViewTest : RobolectricTest() {

    @get:Rule
    val activityRule = activityScenarioRule<ViewTestActivity>()

    @get:Rule
    val roborazziRule = RoborazziRule(
        captureRoot = onView(isRoot()),
        options = RoborazziRule.Options(
            captureType = RoborazziRule.CaptureType.LastImage(),
            outputDirectoryPath = "src/androidUnitTest/snapshots/images",
        )
    )

    @Test
    fun imageView() {
        val url = "https://www.example.com/image.jpg"
        val engine = FakeImageLoaderEngine.Builder()
            .intercept(url, ColorImage(Color.Red.toArgb(), width = 100, height = 100))
            .build()
        val activity = activityRule.scenario.activity
        val imageLoader = ImageLoader.Builder(activity)
            .components { add(engine) }
            .build()
        val imageView = activity.imageView
        imageView.scaleType = ImageView.ScaleType.CENTER
        val request = ImageRequest.Builder(activity)
            .data(url)
            .target(imageView)
            .build()

        // Don't suspend to test that the image view is updated synchronously.
        imageLoader.enqueue(request)
    }
}
