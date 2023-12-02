package coil3.size

import coil3.request.ImageRequest

/**
 * Represents the required precision for the size of an image in an image request.
 *
 * @see ImageRequest.Builder.precision
 */
enum class Precision {

    /**
     * Require that the loaded image's dimensions match the request's size and scale exactly.
     */
    EXACT,

    /**
     * Allow the size of the loaded image to not match the requested dimensions exactly.
     * This enables several optimizations:
     *
     * - If the requested dimensions are larger than the original size of the image,
     *   it will be loaded using its original dimensions. This uses less memory.
     * - If the image is present in the memory cache at a larger size than the request's dimensions,
     *   it will be returned. This increases the hit rate of the memory cache.
     *
     * Prefer this option if your target can scale the loaded image (e.g. `ImageView`).
     */
    INEXACT,

    /**
     * Allow Coil to automatically determine if the size needs to be exact for this request.
     *
     * This is the default value for [ImageRequest.Builder.precision].
     */
    AUTOMATIC,
}
