package coil.size

import android.os.Parcelable
import androidx.annotation.Px
import coil.request.ImageRequest
import kotlinx.parcelize.Parcelize

/**
 * Represents the target size of an image request.
 *
 * @see ImageRequest.Builder.size
 * @see SizeResolver.size
 */
public sealed class Size : Parcelable

/** Represents the width and height of the source image. */
@Parcelize
public object OriginalSize : Size() {
    override fun toString(): String = "coil.size.OriginalSize"
}

/** A positive width and height in pixels. */
@Parcelize
public data class PixelSize(
    @Px val width: Int,
    @Px val height: Int
) : Size() {

    init {
        require(width > 0 && height > 0) { "width and height must be > 0." }
    }
}
