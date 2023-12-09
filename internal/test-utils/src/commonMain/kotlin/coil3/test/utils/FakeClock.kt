package coil3.test.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FakeClock(var epochMillis: Long = 0) : Clock {
    override fun now() = Instant.fromEpochMilliseconds(epochMillis)
}
