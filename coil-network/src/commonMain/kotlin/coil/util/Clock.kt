package coil.util

import kotlin.js.JsName
import kotlin.jvm.JvmName

@JsName("newClock")
@JvmName("newClock")
fun Clock(): Clock = object : Clock {
    override fun epochMillis() = getTimeMillis()
}

/** A simple interface for fetching the time. */
interface Clock {
    fun epochMillis(): Long
}
