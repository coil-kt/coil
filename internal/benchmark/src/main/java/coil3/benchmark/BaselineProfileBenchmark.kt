package coil3.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun noneCompilation() {
        startup(CompilationMode.None())
    }

    @Test
    fun baselineProfile() {
        startup(CompilationMode.Partial(BaselineProfileMode.Require))
    }

    @Test
    fun fullCompilation() {
        startup(CompilationMode.Full())
    }

    private fun startup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE_NAME,
            metrics = listOf(
                FrameTimingMetric(),
                StartupTimingMetric(),
            ),
            iterations = 3,
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
            setupBlock = { pressHome() },
            measureBlock = { startActivityAndWait() },
        )
    }
}
