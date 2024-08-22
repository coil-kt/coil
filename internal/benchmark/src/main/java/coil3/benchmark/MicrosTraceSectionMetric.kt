package coil3.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.perfetto.ExperimentalPerfettoTraceProcessorApi
import androidx.benchmark.perfetto.PerfettoTraceProcessor

/**
 * TraceSectionMetric to give average/sum in microseconds measurements.
 */
@OptIn(ExperimentalMetricApi::class, ExperimentalPerfettoTraceProcessorApi::class)
class MicrosTraceSectionMetric(
    private val sectionName: String,
    private vararg val mode: Mode,
    private val label: String = sectionName,
    private val targetPackageOnly: Boolean = true,
) : TraceMetric() {

    enum class Mode {
        Sum,
        Average
    }

    @Suppress("RestrictedApi")
    override fun getMeasurements(
        captureInfo: CaptureInfo,
        traceSession: PerfettoTraceProcessor.Session,
    ): List<Measurement> {
        val slices = traceSession.querySlices(
            sectionName,
            packageName = if (targetPackageOnly) captureInfo.targetPackageName else null,
        )

        return mode.flatMap { m ->
            when (m) {
                Mode.Sum -> listOf(
                    Measurement(
                        name = sectionName + "_µs",
                        // note, this duration assumes non-reentrant slices
                        data = slices.sumOf { it.dur } / 1_000.0,
                    ),
                    Measurement(
                        name = sectionName + "Count",
                        data = slices.size.toDouble(),
                    ),
                )

                Mode.Average -> listOf(
                    Measurement(
                        name = label + "Average_µs",
                        data = slices.sumOf { it.dur } / 1_000.0 / slices.size,
                    ),
                )
            }
        }
    }
}
