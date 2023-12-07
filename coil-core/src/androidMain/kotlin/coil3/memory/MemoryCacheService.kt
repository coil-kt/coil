package coil3.memory

import coil3.memory.MemoryCacheService.Companion.EXTRA_TRANSFORMATION_INDEX
import coil3.memory.MemoryCacheService.Companion.EXTRA_TRANSFORMATION_SIZE
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.request.transformations
import coil3.util.forEachIndexedIndices

internal actual fun createComplexMemoryCacheKeyExtras(
    request: ImageRequest,
    options: Options,
): Map<String, String> {
    var memoryCacheKeyExtras = request.memoryCacheKeyExtras
    if (request.transformations.isNotEmpty()) {
        val mutableMemoryCacheKeyExtras = memoryCacheKeyExtras.toMutableMap()
        request.transformations.forEachIndexedIndices { index, transformation ->
            mutableMemoryCacheKeyExtras[EXTRA_TRANSFORMATION_INDEX + index] = transformation.cacheKey
        }
        mutableMemoryCacheKeyExtras[EXTRA_TRANSFORMATION_SIZE] = options.size.toString()
        memoryCacheKeyExtras = mutableMemoryCacheKeyExtras
    }
    return memoryCacheKeyExtras
}
