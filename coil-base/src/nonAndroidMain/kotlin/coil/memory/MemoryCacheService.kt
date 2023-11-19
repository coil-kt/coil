package coil.memory

import coil.request.ImageRequest
import coil.request.Options

internal actual fun createComplexMemoryCacheKeyExtras(
    request: ImageRequest,
    options: Options,
): Map<String, String> {
    return request.memoryCacheKeyExtras
}
