package coil3.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import coil3.benchmark.BuildConfig.PROJECT
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("KotlinConstantConditions") // BuildConfig constant can change.
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "sample.$PROJECT",
    ) {
        pressHome()
        startActivityAndWait()
        UiDevice.getInstance(getInstrumentation())
            .findObject(
                if (PROJECT == "compose") {
                    By.res("list")
                } else {
                    By.res(packageName, "list")
                },
            )
            .fling(Direction.DOWN, 3000)
        device.waitForIdle()
    }
}
