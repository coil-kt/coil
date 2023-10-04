package coil.util

internal inline val Any.identityHashCode: Int
    get() = System.identityHashCode(this)

internal actual fun getTimeMillis() = System.currentTimeMillis()
