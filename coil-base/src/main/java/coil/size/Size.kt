@file:JvmName("-Sizes")

package coil.size

import androidx.annotation.Px
import coil.request.ImageRequest

/**
 * Represents the target size of an image request.
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
         * A [Size] whose width and height are equal to the original dimensions of the source image.
         */
        @JvmField val ORIGINAL = Size(Dimension.Original, Dimension.Original)
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
