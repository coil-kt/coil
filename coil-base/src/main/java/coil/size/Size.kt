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

    companion object {
        /**
         * A [Size] whose width and height are equal to the original dimensions of the source image.
         */
        @JvmField val ORIGINAL = Size(Dimension.Original, Dimension.Original)
    }
}

/**
 * Create a [Size] with pixel values for both width and height.
 */
fun Size(@Px width: Int, @Px height: Int) = Size(Dimension(width), Dimension(height))

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
    message = "Migrate to 'coil.size.Size'.",
    replaceWith = ReplaceWith("Size(width, height)", "coil.size.Size")
)
@JvmName("PixelSize")
inline fun PixelSize(@Px width: Int, @Px height: Int) = Size(width, height)

@Deprecated(
    message = "Migrate to 'coil.size.Size.ORIGINAL'.",
    replaceWith = ReplaceWith("Size.ORIGINAL", "coil.size.Size")
)
inline val OriginalSize: Size
    @JvmName("OriginalSize") get() = Size.ORIGINAL
