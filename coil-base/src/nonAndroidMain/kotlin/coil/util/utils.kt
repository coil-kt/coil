package coil.util

import coil.request.ImageRequest
import coil.size.Precision

internal actual fun println(level: Logger.Level, tag: String, message: String) {
    println(message)
}

internal actual val ImageRequest.allowInexactSize: Boolean
    get() = when (precision) {
        Precision.EXACT -> false
        Precision.INEXACT,
        Precision.AUTOMATIC -> true
    }
