package coil.network

class FakeClock(var timestamp: Long = 0) : Clock {
    override fun epochMillis() = timestamp
}
