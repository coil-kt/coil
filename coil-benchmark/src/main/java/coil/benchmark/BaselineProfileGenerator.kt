package coil.benchmark

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
    @Suppress("KotlinConstantConditions") // BuildConfig constant can change.
    fun generate() = baselineProfileRule.collectBaselineProfile(
        packageName = BuildConfig.PACKAGE_NAME,
        filterPredicate = if (BuildConfig.PACKAGE_NAME == "coil.sample.compose") {
            // Only include Compose-specific rules in the coil-compose module.
            { it == "coil.compose" }
        } else {
            // Include all Coil rules in the coil-base module.
            { it == "coil" }
        },
    ) {
        pressHome()
        startActivityAndWait()
        UiDevice.getInstance(getInstrumentation())
            .findObject(By.res("list"))
            .fling(Direction.DOWN, 3000)
        device.waitForIdle()
    }
}
