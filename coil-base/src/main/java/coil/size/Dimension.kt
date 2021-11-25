@file:JvmName("Dimensions")
@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package coil.size

import androidx.annotation.Px

/**
 * Represents either the width or height of a [Size].
 */
sealed class Dimension {

    /**
     * Represents a fixed, positive number of pixels.
     */
    data class Pixels(@Px val pixels: Int) : Dimension() {

        init {
            require(pixels > 0) { "value must be > 0." }
        }
    }

    /**
     * Represents the original value of the source image.
     *
     * i.e. if the image's original dimensions are 400x600 and this is used as the width, this
     * should be treated as 400 pixels.
     */
    object Original : Dimension()
}

/**
 * Convenience function to create a [Dimension.Pixels].
 */
@JvmName("create")
inline fun Dimension(@Px pixels: Int) = Dimension.Pixels(pixels)

/**
 * If this is a [Dimension.Pixels] value, return its number of pixels. Else, invoke and return
 * the value from [block].
 */
inline fun Dimension.pixelsOrElse(block: () -> Int): Int {
    return if (this is Dimension.Pixels) pixels else block()
}
