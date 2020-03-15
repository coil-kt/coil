package coil.size

import androidx.annotation.MainThread
import coil.request.RequestBuilder

/**
 * An interface for measuring the target size for an image request.
 *
 * @see RequestBuilder.size
 */
interface SizeResolver {

    companion object {
        /** Create a [SizeResolver] with a fixed [size]. */
        @JvmStatic
        @JvmName("create")
        operator fun invoke(size: Size): SizeResolver {
            return object : SizeResolver {
                override suspend fun size() = size
            }
        }
    }

    /** Return the [Size] that the image should be loaded at. */
    @MainThread
    suspend fun size(): Size
}
