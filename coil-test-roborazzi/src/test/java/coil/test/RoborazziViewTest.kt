package coil.test

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.ImageLoader
import coil.request.ImageRequest
import coil.util.ViewTestActivity
import coil.util.activity
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoborazziViewTest {

    @get:Rule
    val activityRule = activityScenarioRule<ViewTestActivity>()

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

        // https://github.com/takahirom/roborazzi/issues/9
        onView(isRoot())
            .captureRoboImage("src/test/snapshots/images/coil_test_RoborazziViewTest_loadView.png")
    }
}
