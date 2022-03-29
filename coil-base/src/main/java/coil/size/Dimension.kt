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

        override fun toString() = "Dimension.Pixels(px=$px)"
    }

    /**
     * Represents an undefined number of pixels.
     *
     * This value is typically used in cases where a dimension is unbounded (e.g. [WRAP_CONTENT]).
     *
     * NOTE: If at least one dimension is undefined [Options.scale] is always [Scale.FIT].
     */
    object Undefined : Dimension() {
        override fun toString() = "Dimension.Undefined"
    }
}

/**
 * Convenience function to create a [Dimension.Pixels].
 */
inline fun Dimension(@Px px: Int) = Dimension.Pixels(px)

/**
 * If this is a [Dimension.Pixels] value, return its number of pixels. Else, invoke and return
 * the value from [block].
 */
inline fun Dimension.pxOrElse(block: () -> Int): Int {
    return if (this is Dimension.Pixels) px else block()
}
