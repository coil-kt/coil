@file:JvmName("-Dimensions")
@file:Suppress("NOTHING_TO_INLINE")

package coil.size

import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.Px
import coil.request.Options

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

        override fun toString() = px.toString()
    }

    /**
     * Represents the original value of the source image.
     *
     * i.e. if the image's original dimensions are 400x600 and this is used as the width, this
     * should be treated as 400 pixels.
     *
     * This value is typically used in cases where a dimension is unbounded (e.g. [WRAP_CONTENT],
     * `Constraints.Infinity`).
     *
     * NOTE: If at least one dimension is [Original], [Options.scale] is always [Scale.FIT].
     */
    object Original : Dimension() {
        override fun toString() = "Dimension.Original"
    }
}

/**
 * Convenience function to create a [Dimension.Pixels].
 */
inline fun Dimension(@Px px: Int) = Dimension.Pixels(px)

/**
 * If this is a [Dimension.Pixels] value, return its pixel value. Else, invoke and return
 * the value from [block].
 */
inline fun Dimension.pxOrElse(block: () -> Int): Int {
    return if (this is Dimension.Pixels) px else block()
}
