package coil3.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FakeClock(var epochMillis: Long = 0) : Clock {
    override fun now() = Instant.fromEpochMilliseconds(epochMillis)
}
