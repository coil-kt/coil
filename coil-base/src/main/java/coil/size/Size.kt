package coil.size

import androidx.annotation.Px
import coil.request.RequestBuilder

/**
 * Represents the target size of an image request.
 *
 * @see RequestBuilder.size
 * @see SizeResolver.size
 */
sealed class Size

/** Represents the width and height of the source image. */
object OriginalSize : Size() {
    override fun toString() = "OriginalSize"
}

/** A positive width and height in pixels. */
data class PixelSize(
    @Px val width: Int,
    @Px val height: Int
) : Size() {

    init {
        require(width > 0 && height > 0) { "Width and height must be > 0." }
    }
}
