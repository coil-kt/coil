package coil.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.benchmark.BuildConfig.PROJECT
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() {
        startup(CompilationMode.None())
    }

    @Test
    fun startupPartialCompilation() {
        startup(CompilationMode.Partial(BaselineProfileMode.Disable, warmupIterations = 3))
    }

    @Test
    fun startupBaselineProfile() {
        startup(CompilationMode.Partial(BaselineProfileMode.Require))
    }

    @Test
    fun startupFullCompilation() {
        startup(CompilationMode.Full())
    }

    private fun startup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "sample.$PROJECT",
            metrics = listOf(StartupTimingMetric()),
            iterations = 10,
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
            setupBlock = { pressHome() },
            measureBlock = { startActivityAndWait() },
        )
    }
}
