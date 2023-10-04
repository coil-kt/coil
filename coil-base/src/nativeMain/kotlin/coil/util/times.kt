package coil.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.timespec

@OptIn(ExperimentalForeignApi::class)
internal actual fun getTimeMillis(): Long = memScoped {
    val timeHolder = alloc<timespec>()
    clock_gettime(CLOCK_REALTIME.convert(), timeHolder.ptr)
    timeHolder.tv_sec * 1_000L + timeHolder.tv_nsec / 1_000_000L
}
