package coil3

/**
 * An image that draws a [color].
 *
 * By default the image has no intrinsic size and will fill its canvas. Set [width] and [height]
 * to a positive value to draw a square with those dimensions.
 *
 * @param color The ARGB hex color to draw with the format `0xAARRGGBB.toInt()`.
 *  Tip: Use Compose's color class: `ColorImage(Color.Black.toArgb())`.
 */
expect class ColorImage(
    color: Int = 0xFF000000.toInt(),
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
}
