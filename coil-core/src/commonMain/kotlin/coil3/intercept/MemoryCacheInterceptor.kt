package coil3.intercept

import coil3.memory.MemoryCacheService
import coil3.request.ImageResult
import coil3.request.RequestService
import coil3.util.eventListener
import coil3.util.isPlaceholderCached
import kotlinx.coroutines.withContext

internal class MemoryCacheInterceptor(
    private val requestService: RequestService,
    private val memoryCacheService: MemoryCacheService,
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val size = chain.size
        val eventListener = chain.eventListener

        // Check the memory cache.
        val options = requestService.options(request, size)
        val mappedData = memoryCacheService.mapData(request, options, eventListener)
        val cacheKey = memoryCacheService.newCacheKey(request, mappedData, options, eventListener)
        val cacheValue = cacheKey?.let { memoryCacheService.getCacheValue(request, it, size, options.scale) }

        // Return the value from the memory cache.
        if (cacheValue != null) {
            return memoryCacheService.newResult(request, cacheKey, cacheValue, chain.isPlaceholderCached)
        }

        return withContext(request.interceptorCoroutineContext) {
            (chain as RealInterceptorChain).withMemoryCacheKey(cacheKey).proceed()
        }
    }
}
