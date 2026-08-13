package coil3.gif

internal actual fun validateRepeatCount(repeatCount: Int) {
    require(repeatCount >= ENCODED_LOOP_COUNT) { "Invalid repeatCount: $repeatCount" }
}
