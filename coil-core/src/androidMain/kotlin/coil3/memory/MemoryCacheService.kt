package coil3.memory

import coil3.request.ImageRequest
import coil3.request.transformations

internal actual fun ImageRequest.needsSizeInCacheKey(): Boolean {
    return transformations.isNotEmpty()
}
