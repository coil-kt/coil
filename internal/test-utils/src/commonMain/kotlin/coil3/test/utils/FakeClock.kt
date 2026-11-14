package coil3.test.utils

import kotlin.time.Clock
import kotlin.time.Instant

class FakeClock(var epochMillis: Long = 0) : Clock {
    override fun now() = Instant.fromEpochMilliseconds(epochMillis)
}
