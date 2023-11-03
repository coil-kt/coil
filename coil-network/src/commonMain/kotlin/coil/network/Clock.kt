package coil.network

import io.ktor.util.date.getTimeMillis
import kotlin.js.JsName

@JsName("newClock")
fun Clock() = object : Clock {
    override fun epochMillis() = getTimeMillis()
}

interface Clock {
    fun epochMillis(): Long
}
