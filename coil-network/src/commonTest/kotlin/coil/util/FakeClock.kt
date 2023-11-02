package coil.util

class FakeClock(var currentTimeMillis: Long = 0) : Clock {
    override fun epochMillis() = currentTimeMillis
}
