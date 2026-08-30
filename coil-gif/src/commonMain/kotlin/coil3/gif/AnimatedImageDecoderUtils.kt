package coil3.gif

object AnimatedImageDecoderUtils {

    /**
     * Pass to [repeatCount] to repeat the animation indefinitely.
     */
    const val REPEAT_INFINITE = -1

    /**
     * Pass to [repeatCount] to repeat according to the file's encoded LoopCount metadata.
     *
     * This is only supported in `AnimatedImageDecoder` on Android and `AnimatedSkiaImageDecoder`
     * on non-Android platforms.
     */
    const val ENCODED_LOOP_COUNT = -2
}
