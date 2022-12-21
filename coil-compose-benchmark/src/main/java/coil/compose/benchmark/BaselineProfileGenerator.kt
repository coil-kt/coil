package coil.compose.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collectBaselineProfile(
        packageName = "sample.compose",
        filterPredicate = { it == "coil" },
    ) {
        pressHome()
        startActivityAndWait()
        UiDevice.getInstance(getInstrumentation())
            .findObject(By.res("scrollableContent"))
            .fling(Direction.DOWN, 3000)
        device.waitForIdle()
    }
}
