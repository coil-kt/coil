package coil3.test

import coil3.Canvas
import coil3.Image

/**
 * A simple [Image] that draws a 100x100 black square by default.
 */
expect class FakeImage(
    width: Int = 100,
    height: Int = 100,
    size: Long = 4L * width * height,
    shareable: Boolean = true,
    color: Int = 0x000000,
) : Image {
    override val width: Int
    override val height: Int
    override val size: Long
    override val shareable: Boolean
    val color: Int
    override fun draw(canvas: Canvas)
}
