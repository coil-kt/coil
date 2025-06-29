package coil3.svg

import coil3.Image
import coil3.annotation.ExperimentalCoilApi
import coil3.request.Options
import coil3.svg.internal.parseSvg
import kotlin.jvm.JvmField
import okio.BufferedSource

/**
 * Represents an SVG with mutable properties.
 *
 * NOTE: This interface is provided to enable custom SVG parsing/rendering implementations, however
 * it intentionally only provides hooks for the properties Coil uses. Additionally, these classes
 * are very likely to change in the future.
 */
@ExperimentalCoilApi
interface Svg {
    var viewBox: ViewBox?
    val width: Float
    val height: Float

    fun width(value: String)
    fun height(value: String)
    fun options(options: Options)

    /** Wrap this SVG as an [Image]. */
    fun asImage(
        width: Int = this.width.toInt(),
        height: Int = this.height.toInt(),
    ): Image

    data class ViewBox(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    )

    /**
     * An SVG parser that converts bytes into an [Svg].
     *
     * @see parseSvg for an example implementation.
     */
    fun interface Parser {
        fun parse(source: BufferedSource): Svg

        companion object {
            @JvmField val DEFAULT = Parser(::parseSvg)
        }
    }
}

val Svg.ViewBox.width: Float
    get() = right - left

val Svg.ViewBox.height: Float
    get() = bottom - top
