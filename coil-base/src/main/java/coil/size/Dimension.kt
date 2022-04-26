@file:JvmName("-Dimensions")

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
     * Represents an undefined pixel value.
     *
     * E.g. given `Size(400, Dimension.Undefined)`, the image should be loaded to fit/fill a width
     * of 400 pixels irrespective of the image's height.
     *
     * This value is typically used in cases where a dimension is unbounded (e.g. [WRAP_CONTENT],
     * `Constraints.Infinity`).
     *
     * NOTE: If either dimension is [Undefined], [Options.scale] is always [Scale.FIT].
     */
    object Undefined : Dimension() {
        override fun toString() = "Dimension.Undefined"
    }
}

/** Create a [Dimension.Pixels] value with [px] number of pixels. */
fun Dimension(@Px px: Int) = Dimension.Pixels(px)

/**
 * If this is a [Dimension.Pixels] value, return its pixel value.
 * Else, invoke and return the value from [block].
 */
inline fun Dimension.pxOrElse(block: () -> Int): Int {
    return if (this is Dimension.Pixels) px else block()
}
