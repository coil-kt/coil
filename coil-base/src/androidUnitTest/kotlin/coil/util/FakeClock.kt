package coil.util

class FakeClock(var epochMillis: Long = 0) : Clock {
    override fun epochMillis() = epochMillis
}
