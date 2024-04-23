package coil.benchmark

import android.graphics.Point
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.perfetto.ExperimentalPerfettoTraceProcessorApi
import androidx.benchmark.perfetto.PerfettoTraceProcessor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import coil.benchmark.BuildConfig.PROJECT
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @RequiresApi(Build.VERSION_CODES.N)
    @Test
    fun baselineProfile() {
        startup(CompilationMode.Partial(BaselineProfileMode.Require))
    }

    @Test
    fun fullCompilation() {
        startup(CompilationMode.Full())
    }

    @OptIn(ExperimentalMetricApi::class)
    private fun startup(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "sample.$PROJECT",
            metrics = listOf(
                FrameTimingMetric(),
                StartupTimingMetric(),
                TraceSectionMetric(
                    "coil.compose.rememberAsyncImagePainter%",
                    TraceSectionMetric.Mode.Sum,
                ),
                AverageTraceSectionMetric("coil.compose.rememberAsyncImagePainter%"),
            ),
            iterations = 1,
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
            measureBlock = {
                startActivityAndWait()
                val list = device.findObject(By.res("list"))
                list.setGestureMargin(device.displayWidth / 5)
                list.drag(Point(list.visibleBounds.centerX(), list.visibleBounds.top))
                Thread.sleep(300)
            },
        )
    }
}

@OptIn(ExperimentalMetricApi::class)
class AverageTraceSectionMetric(
    private val sectionName: String,
    private val label: String = sectionName,
    private val targetPackageOnly: Boolean = true,
) : TraceMetric() {

    @ExperimentalPerfettoTraceProcessorApi
    @Suppress("RestrictedApi")
    override fun getResult(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session,
    ): List<Measurement> {
        val slices = traceSession.querySlices(
            sectionName,
            packageName = if (targetPackageOnly) captureInfo.targetPackageName else null,
        )

        return listOf(
            Measurement(
                name = label + "AverageMs",
                data = slices.sumOf { it.dur } / 1_000_000.0 / slices.size,
            ),
        )
    }
}
