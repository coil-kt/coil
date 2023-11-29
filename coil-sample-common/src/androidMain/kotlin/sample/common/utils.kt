package sample.common

import coil.Extras
import coil.request.videoFrameMicros

actual val Extras.Key.Companion.videoFrameMicros: Extras.Key<Long>
    get() = videoFrameMicros
