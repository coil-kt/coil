package coil.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import coil.benchmark.BuildConfig.PROJECT
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("KotlinConstantConditions") // BuildConfig constant can change.
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collectBaselineProfile(
        packageName = "sample.$PROJECT",
        filterPredicate = newFilterPredicate(),
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

    private fun newFilterPredicate(): (String) -> Boolean {
        // Only include Compose-specific rules in the coil-compose module.
        val packageName = if (PROJECT == "compose") "coil/compose/" else "coil/"
        return { line -> packageName in line && "sample/" !in line }
    }
}
