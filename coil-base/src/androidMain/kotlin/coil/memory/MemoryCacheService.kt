package coil.memory

import coil.request.ImageRequest
import coil.request.Options
import coil.request.transformations
import coil.util.forEachIndexedIndices

internal actual fun createComplexMemoryCacheKeyExtras(
    request: ImageRequest,
    options: Options,
): Map<String, String> {
    var memoryCacheKeyExtras = request.memoryCacheKeyExtras
    if (request.transformations.isNotEmpty()) {
        val mutableMemoryCacheKeyExtras = memoryCacheKeyExtras.toMutableMap()
        request.transformations.forEachIndexedIndices { index, transformation ->
            mutableMemoryCacheKeyExtras[MemoryCacheService.EXTRA_TRANSFORMATION_INDEX + index] = transformation.cacheKey
        }
        mutableMemoryCacheKeyExtras[MemoryCacheService.EXTRA_TRANSFORMATION_SIZE] = options.size.toString()
        memoryCacheKeyExtras = mutableMemoryCacheKeyExtras
    }
    return memoryCacheKeyExtras
}
