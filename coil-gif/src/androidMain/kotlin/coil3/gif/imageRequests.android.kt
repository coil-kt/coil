package coil3.gif

import android.os.Build.VERSION.SDK_INT

internal actual fun validateRepeatCount(repeatCount: Int) {
    val minimum = if (SDK_INT >= 28) {
        AnimatedImageDecoder.ENCODED_LOOP_COUNT
    } else {
        MovieDrawable.REPEAT_INFINITE
    }
    require(repeatCount >= minimum) { "Invalid repeatCount: $repeatCount" }
}
