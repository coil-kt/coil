package coil3.memory

import coil3.request.ImageRequest

internal actual fun ImageRequest.needsSizeInCacheKey() = false
