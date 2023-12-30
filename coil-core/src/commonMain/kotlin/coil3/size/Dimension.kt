package coil3.size

import coil3.annotation.Data
import coil3.request.Options
import kotlin.jvm.JvmField

/**
 * Represents either the width or height of a [Size].
 */
sealed interface Dimension {

    /**
     * Represents a fixed, positive number of pixels.
     */
    @Data
    class Pixels(@JvmField val px: Int) : Dimension {

        init {
            require(px > 0) { "px must be > 0." }
        }
    }

    /**
     * Represents an undefined pixel value.
     *
     * E.g. given `Size(400, Dimension.Undefined)`, the image should be loaded to fit/fill a width
     * of 400 pixels irrespective of the image's height.
     *
     * This value is typically used in cases where a dimension is unbounded
     * (e.g. `ViewGroup.LayoutParams.WRAP_CONTENT`, `Constraints.Infinity`).
     *
     * NOTE: If either dimension is [Undefined], [Options.scale] is always [Scale.FIT].
     */
    data object Undefined : Dimension
}

/** Create a [Dimension.Pixels] value with [px] number of pixels. */
fun Dimension(px: Int) = Dimension.Pixels(px)

/**
 * If this is a [Dimension.Pixels] value, return its pixel value.
 * Else, invoke and return the value from [block].
 */
inline fun Dimension.pxOrElse(block: () -> Int): Int {
    return if (this is Dimension.Pixels) px else block()
}
