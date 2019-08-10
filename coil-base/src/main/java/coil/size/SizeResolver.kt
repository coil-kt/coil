package coil.size

import coil.request.RequestBuilder

/**
 * An interface for measuring the target size for an image request.
 *
 * @see RequestBuilder.size
 */
interface SizeResolver {

    companion object {
        /**
         * Construct a [SizeResolver] instance with a fixed [size].
         */
        operator fun invoke(size: Size): SizeResolver {
            return object : SizeResolver {
                override suspend fun size() = size
            }
        }
    }

    /**
     * Return the [Size] that the image should be loaded at.
     */
    suspend fun size(): Size
}
