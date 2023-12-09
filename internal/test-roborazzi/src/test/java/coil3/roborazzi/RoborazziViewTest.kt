package coil3.roborazzi

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.target
import coil3.test.FakeImageLoaderEngine
import coil3.test.intercept
import coil3.test.utils.ViewTestActivity
import coil3.test.utils.activity
import com.github.takahirom.roborazzi.RoborazziRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoborazziViewTest {

    @get:Rule
    val activityRule = activityScenarioRule<ViewTestActivity>()

    @get:Rule
    val roborazziRule = RoborazziRule(
        captureRoot = onView(isRoot()),
        options = RoborazziRule.Options(
            captureType = RoborazziRule.CaptureType.LastImage(),
            outputDirectoryPath = "src/test/snapshots/images",
        )
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
