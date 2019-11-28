package coil.size

import android.widget.ImageView
import coil.memory.RequestService
import coil.request.RequestBuilder

/**
 * @see RequestBuilder.precision
 */
enum class Precision {

    /**
     * Require that the loaded image's dimensions match the request's size and scale exactly.
     */
    EXACT,

    /**
     * Allow the size of the loaded image to not match the requested dimensions exactly. This enables several optimizations:
     *
     * - If the requested dimensions are larger than the original size of the image,
     *   it will be loaded using its original dimensions. This uses less memory.
     * - If the image is present in the memory cache at a larger size than the request's dimensions, it will be returned.
     *   This increases the hit rate of the memory cache.
     *
     * Prefer this option if your target can scale the loaded image (e.g. [ImageView]).
     */
    INEXACT,

    /**
     * Allow Coil to automatically determine if the size needs to be exact for this request
     * using the logic in [RequestService.requireExactSize].
     *
     * Currently, the size will always be exact unless the image is being loaded into an [ImageView].
     */
    AUTOMATIC
}
