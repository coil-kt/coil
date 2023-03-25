package coil.test

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.view.updateLayoutParams
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.ImageLoader
import coil.request.ImageRequest
import coil.util.ViewTestActivity
import coil.util.activity
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
            outputDirectoryPath = "src/test/snapshots/images",
        )
    )

    // TODO: Update screenshot once https://github.com/takahirom/roborazzi/issues/9 is fixed.
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
        val request = ImageRequest.Builder(activity)
            .data(url)
            .target(activity.imageView)
            .build()
        activity.imageView.updateLayoutParams {
            width = MATCH_PARENT
            height = MATCH_PARENT
        }

        // Don't suspend to test that the image view is updated synchronously.
        imageLoader.enqueue(request)
    }
}
