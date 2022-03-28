@file:JvmName("-Sizes")
@file:Suppress("NOTHING_TO_INLINE", "unused")

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

    /** Create a [Size] with a pixel value for width. */
    constructor(@Px width: Int, height: Dimension) : this(Dimension(width), height)

    /** Create a [Size] with a pixel value for height. */
    constructor(width: Dimension, @Px height: Int) : this(width, Dimension(height))

    /** Create a [Size] with pixel values for both width and height. */
    constructor(@Px width: Int, @Px height: Int) : this(Dimension(width), Dimension(height))

    companion object {
        /**
         * A [Size] whose width and height are equal to the original dimensions of the source image.
         */
        @JvmField val ORIGINAL = Size(Dimension.Undefined, Dimension.Undefined)
    }
}

/**
 * Return true if this size is equal to [Size.ORIGINAL]. Else, return false.
 */
val Size.isOriginal: Boolean
    get() = this == Size.ORIGINAL

@Deprecated(
    message = "Migrate to 'coil.size.Size'.",
    replaceWith = ReplaceWith("Size", "coil.size.Size")
)
typealias PixelSize = Size

@Deprecated(
    message = "Migrate to 'coil.size.Size.ORIGINAL'.",
    replaceWith = ReplaceWith("Size.ORIGINAL", "coil.size.Size")
)
inline val OriginalSize: Size
    @JvmName("OriginalSize") get() = Size.ORIGINAL
