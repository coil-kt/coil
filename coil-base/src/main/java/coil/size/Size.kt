@file:JvmName("-Sizes")

package coil.size

import androidx.annotation.Px
import coil.request.ImageRequest

/**
 * Represents the target size of an image request.
 *
 * Each [Size] is composed of two [Dimension]s, [width] and [height]. Each dimension determines
 * if and how much the source image should be scaled. A [Dimension] can either be a fixed pixel
 * value or [Dimension.Undefined]. Examples:
 *
 * - Given `Size(400, 600)`, the image will be loaded to fit/fill a width of 400 pixels and a
 *   height of 600 pixels.
 * - Given `Size(400, Dimension.Undefined)`, the image will be loaded to fit/fill a width of 400
 *   pixels.
 * - Given `Size(Dimension.Undefined, Dimension.Undefined)`, the image will not be scaled to
 *   fit/fill either width or height. i.e. it will be loaded at its original width/height.
 *
 * @see ImageRequest.Builder.size
 * @see SizeResolver.size
 */
data class Size(
    val width: Dimension,
    val height: Dimension,
) {

    companion object {
        /**
         * A [Size] whose width and height are undefined - meaning the source image will be loaded
         * without any scaling.
         */
        @JvmField val ORIGINAL = Size(Dimension.Undefined, Dimension.Undefined)
    }
}

/** Create a [Size] with a pixel value for width. */
fun Size(@Px width: Int, height: Dimension) = Size(Dimension(width), height)

/** Create a [Size] with a pixel value for height. */
fun Size(width: Dimension, @Px height: Int) = Size(width, Dimension(height))

/** Create a [Size] with pixel values for both width and height. */
fun Size(@Px width: Int, @Px height: Int) = Size(Dimension(width), Dimension(height))

/** Return true if this size is equal to [Size.ORIGINAL]. Else, return false. */
val Size.isOriginal: Boolean get() = this == Size.ORIGINAL

@Deprecated(
    message = "Migrate to 'coil.size.Size.ORIGINAL'.",
    replaceWith = ReplaceWith("Size.ORIGINAL", "coil.size.Size")
)
val OriginalSize: Size get() = Size.ORIGINAL
