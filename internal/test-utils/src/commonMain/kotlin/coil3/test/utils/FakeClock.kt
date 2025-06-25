package coil3.test.utils

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@ExperimentalTime
class FakeClock(var epochMillis: Long = 0) : Clock {
    override fun now() = Instant.fromEpochMilliseconds(epochMillis)
}
