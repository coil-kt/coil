package coil.benchmark

import android.graphics.Point
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import coil.benchmark.BuildConfig.PROJECT
import coil.benchmark.MicrosTraceSectionMetric.Mode.Average
import coil.benchmark.MicrosTraceSectionMetric.Mode.Sum
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
        benchmark(CompilationMode.Partial(BaselineProfileMode.Require))
    }

    @Test
    fun fullCompilation() {
        benchmark(CompilationMode.Full())
    }

    private fun benchmark(compilationMode: CompilationMode) {
        benchmarkRule.measureRepeated(
            packageName = "sample.$PROJECT",
            metrics = listOf(
                FrameTimingMetric(),
                StartupTimingMetric(),
                MicrosTraceSectionMetric(
                    "rememberAsyncImagePainter",
                    Sum, Average,
                ),
                MicrosTraceSectionMetric(
                    "AsyncImagePainter.onRemembered",
                    Sum, Average,
                ),
            ),
            iterations = 20,
            startupMode = StartupMode.COLD,
            compilationMode = compilationMode,
            measureBlock = {
                startActivityAndWait()
                Thread.sleep(3_000)
                val list = device.findObject(By.res("list"))
                list.setGestureMargin(device.displayWidth / 5)
                list.drag(Point(list.visibleBounds.centerX(), list.visibleBounds.top))
                Thread.sleep(300)
            },
        )
    }
}
