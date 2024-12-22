package coil3

import coil3.annotation.ExperimentalCoilApi

/**
 * An image that draws a [color].
 *
 * By default the image has no intrinsic size and will fill its canvas. Set [width] and [height]
 * to a positive value to draw a square with those dimensions.
 *
 * [color] can be set either:
 *
 * - with a custom hex value following the `0xAARRGGBB.toInt()` format (alpha is required)
 * - or it can be set with `androidx.compose.ui.graphics.Color`: `ColorImage(Color.Black.toArgb())`.
 */
@ExperimentalCoilApi
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
