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
        val packageNames = if (PROJECT == "compose") {
            composePackageNames
        } else {
            basePackageNames
        }
        return filter@{ line ->
            val endIndex = line.indexOf(';')
            if (endIndex == -1) {
                return@filter false
            }

            if (line.indexOf("sample/") != -1) {
                return@filter false
            }

            packageNames.any { packageName ->
                val index = line.indexOf(packageName)
                index != -1 && index < endIndex
            }
        }
    }
}

// Only include Compose-specific rules in the coil-compose module.
private val composePackageNames = listOf(
    "coil3/compose/",
)

private val basePackageNames = listOf(
    "coil3/",
    "kotlinx/coroutines/",
    "okio/",
)
