package coil3.test

import coil3.Canvas
import coil3.Image

/**
 * An image that draws a [color].
 *
 * By default the image has no intrinsic size and will fill its canvas. Set [width] and [height]
 * to a positive value to draw a square with those dimensions.
 */
expect class ColorImage(
    color: Int = Black,
    width: Int = -1,
    height: Int = -1,
    size: Long = 0,
    shareable: Boolean = true,
) : Image {
    val color: Int
    override val size: Long
    override val width: Int
    override val height: Int
    override val shareable: Boolean
    override fun draw(canvas: Canvas)

    companion object {
        val Black: Int
        val White: Int
        val Transparent: Int
        val Red: Int
        val Green: Int
        val Blue: Int
    }
}
