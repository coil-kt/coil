package coil.memory

internal class MemoryCacheService(
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache
) {

    operator fun get(key: MemoryCache.Key?): RealMemoryCache.Value? {
        key ?: return null
        return strongMemoryCache.get(key) ?: weakMemoryCache.get(key)
    }
}
