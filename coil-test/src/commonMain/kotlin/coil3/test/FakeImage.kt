package coil3.test

import coil3.Canvas
import coil3.Image
import coil3.test.ColorImage.Companion.Black

/**
 * A simple [Image] that draws a 100x100 black square by default.
 */
@Deprecated(
    message = "This use case is better fulfilled by ColorImage.",
    replaceWith = ReplaceWith("ColorImage(color, width, height, size, shareable)", ["coil3.test.ColorImage"]),
    level = DeprecationLevel.WARNING,
)
expect class FakeImage(
    width: Int = 100,
    height: Int = 100,
    size: Long = 4L * width * height,
    shareable: Boolean = true,
    color: Int = Black,
) : Image {
    override val width: Int
    override val height: Int
    override val size: Long
    override val shareable: Boolean
    val color: Int
    override fun draw(canvas: Canvas)
}
