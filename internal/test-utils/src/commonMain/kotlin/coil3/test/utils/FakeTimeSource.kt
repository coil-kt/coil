package coil3.test.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Simple [TimeSource] that can be manually advanced for tests.
 */
class FakeTimeSource(initial: Duration = ZERO) : TimeSource {
    private var current: Duration = initial

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
