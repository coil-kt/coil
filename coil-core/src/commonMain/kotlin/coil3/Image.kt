package coil3

import kotlin.jvm.JvmOverloads

/**
 * An image that can be drawn on a canvas.
 */
interface Image {

    /**
     * The size of the image in memory in bytes.
     */
    val size: Long

    /**
     * The intrinsic width of the image in pixels or -1 if the image has no intrinsic width.
     */
    val width: Int

    /**
     * The intrinsic height of the image in pixels or -1 if the image has no intrinsic height.
     */
    val height: Int

    /**
     * True if the image can be shared between multiple [Target]s at the same time.
     *
     * For example, a bitmap can be shared between multiple targets if it's immutable.
     * Conversely, an animated image cannot be shared as its internal state is being mutated while
     * its animation is running.
     */
    val shareable: Boolean

    /**
     * Draw the image to a [Canvas].
     */
    fun draw(canvas: Canvas)
}

/**
 * An [Image] that's backed by a [Bitmap].
 */
expect class BitmapImage : Image {
    val bitmap: Bitmap
    override val size: Long
    override val width: Int
    override val height: Int
    override val shareable: Boolean
    override fun draw(canvas: Canvas)
}

/**
 * A grid of pixels.
 */
expect class Bitmap

/**
 * A graphics surface that can be drawn on.
 */
expect class Canvas

/**
 * Convert a [Bitmap] into an [Image].
 */
@JvmOverloads
expect fun Bitmap.asImage(
    shareable: Boolean = true,
): BitmapImage

/**
 * Convert an [Image] into a [Bitmap].
 */
@JvmOverloads
expect fun Image.toBitmap(
    width: Int = this.width,
    height: Int = this.height,
): Bitmap
