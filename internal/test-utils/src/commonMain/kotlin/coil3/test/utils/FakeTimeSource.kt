package coil3.test.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class FakeTimeSource(
    private var current: Duration = ZERO,
) : TimeSource {

    override fun markNow(): TimeMark {
        val start = current
        return object : TimeMark {
            override fun elapsedNow(): Duration = current - start
        }
    }

    fun advanceBy(duration: Duration) {
        current += duration
    }

    fun reset(duration: Duration = ZERO) {
        current = duration
    }
}
