package coil3.test

import coil3.Canvas
import coil3.Image
import coil3.test.internal.Black

/**
 * A simple [Image] that draws a 100x100 black square by default.
 */
expect class FakeImage(
    color: Int = Black,
    width: Int = 100,
    height: Int = 100,
    size: Long = 4L * width * height,
    shareable: Boolean = true,
) : Image {

    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        width: Int,
        height: Int,
        size: Long,
        shareable: Boolean,
        color: Int = Black,
    )

    val color: Int
    override val width: Int
    override val height: Int
    override val size: Long
    override val shareable: Boolean

    override fun draw(canvas: Canvas)
}
