package coil.memory

import coil.ComponentRegistry
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value

internal class RealMemoryCache(
    private val componentRegistry: ComponentRegistry,
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
    private val bitmapReferenceCounter: BitmapReferenceCounter
) : MemoryCache {

    override val size get() = strongMemoryCache.size

    override val maxSize get() = strongMemoryCache.maxSize

    override fun get(key: Key): Value? {
        val value = strongMemoryCache.get(key) ?: weakMemoryCache.get(key)
        return value?.also { bitmapReferenceCounter.invalidate(it.bitmap) }
    }

    override fun remove(key: Key) {
        strongMemoryCache.remove(key)
        weakMemoryCache.remove(key)
    }

    override fun clear() {
        strongMemoryCache.clearMemory()
        weakMemoryCache.clearMemory()
    }
}
