package coil3.memory

import coil3.memory.MemoryCache.Key

internal class RealMemoryCache(
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
) : MemoryCache {

    override val size get() = strongMemoryCache.size

    override val maxSize get() = strongMemoryCache.maxSize

    override val keys get() = strongMemoryCache.keys + weakMemoryCache.keys

    override fun get(key: Key): MemoryCache.Value? {
        val value = strongMemoryCache.get(key) ?: weakMemoryCache.get(key)

        // Remove non-shareable images from the cache when they're returned.
        if (value != null && !value.image.shareable) {
            remove(key)
        }

        return value
    }

    override fun set(key: Key, value: MemoryCache.Value) {
        val size = value.image.size
        check(size >= 0) { "Image size must be non-negative: $size" }

        strongMemoryCache.set(
            key = key,
            image = value.image,
            extras = value.extras,
            size = size,
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

    override fun trimToSize(size: Long) {
        strongMemoryCache.trimToSize(size)
    }

    override fun clear() {
        strongMemoryCache.clear()
        weakMemoryCache.clear()
    }
}
