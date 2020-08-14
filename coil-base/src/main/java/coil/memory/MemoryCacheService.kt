package coil.memory

import coil.bitmap.BitmapReferenceCounter

internal class MemoryCacheService(
    private val referenceCounter: BitmapReferenceCounter,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache
) {

    operator fun get(key: MemoryCache.Key?): RealMemoryCache.Value? {
        key ?: return null
        val value = strongMemoryCache.get(key) ?: weakMemoryCache.get(key)
        if (value != null) referenceCounter.increment(value.bitmap)
        return value
    }
}
