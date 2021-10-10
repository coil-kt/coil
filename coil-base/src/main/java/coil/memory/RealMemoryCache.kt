package coil.memory

import coil.memory.MemoryCache.Key
import coil.util.toImmutableMap

internal class RealMemoryCache(
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache
) : MemoryCache {

    override val size get(): Int = strongMemoryCache.size

    override val maxSize get(): Int = strongMemoryCache.maxSize

    override val keys get(): Set<Key> = strongMemoryCache.keys + weakMemoryCache.keys

    override fun get(key: Key): MemoryCache.Value? {
        return strongMemoryCache.get(key) ?: weakMemoryCache.get(key)
    }

    override fun set(key: Key, value: MemoryCache.Value) {
        // Ensure that stored keys and values are immutable.
        strongMemoryCache.set(
            key = key.copy(extras = key.extras.toImmutableMap()),
            bitmap = value.bitmap,
            extras = value.extras.toImmutableMap()
        )
        // weakMemoryCache.set() is called by strongMemoryCache when
        // a value is evicted from the strong reference cache.
    }

    override fun remove(key: Key): Boolean {
        // Do not short circuit. There is a regression test for this.
        val removedStrong = strongMemoryCache.remove(key)
        val removedWeak = weakMemoryCache.remove(key)
        return removedStrong || removedWeak
    }

    override fun clear() {
        strongMemoryCache.clearMemory()
        weakMemoryCache.clearMemory()
    }

    override fun trimMemory(level: Int) {
        strongMemoryCache.trimMemory(level)
        weakMemoryCache.trimMemory(level)
    }
}
