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
    class Pixels(@JvmField @Px val px: Int) : Dimension() {

        init {
            require(px > 0) { "px must be > 0." }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Pixels && px == other.px
        }

        override fun hashCode() = px

        override fun toString() = "Dimension.Pixels(px=$px)"
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
inline fun Dimension.pxOrElse(block: () -> Int): Int {
    return if (this is Dimension.Pixels) px else block()
}
