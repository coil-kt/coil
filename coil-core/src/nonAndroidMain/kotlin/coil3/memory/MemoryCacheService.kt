package coil3.memory

import coil3.request.ImageRequest
import coil3.request.Options

internal actual fun createComplexMemoryCacheKeyExtras(
    request: ImageRequest,
    options: Options,
): Map<String, String> {
    return request.memoryCacheKeyExtras
}
