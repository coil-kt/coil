package sample.common

import coil3.Extras

actual val Extras.Key.Companion.videoFrameMicros: Extras.Key<Long>
    get() = videoFrameMicrosKey

// This is currently just a placeholder key as video frame decoding is only supported on Android.
private val videoFrameMicrosKey = Extras.Key(default = 0L)
