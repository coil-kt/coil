package coil3.memory

import coil3.memory.MemoryCache.Key
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class RealMemoryCache(
    private val strongMemoryCache: StrongMemoryCache,
    private val weakMemoryCache: WeakMemoryCache,
) : MemoryCache {
    private val lock = SynchronizedObject()

    override val size: Long
        get() = synchronized(lock) { strongMemoryCache.size }

    override val maxSize: Long
        get() = synchronized(lock) { strongMemoryCache.maxSize }

    override val keys: Set<Key>
        get() = synchronized(lock) { strongMemoryCache.keys + weakMemoryCache.keys }

    override fun get(key: Key): MemoryCache.Value? = synchronized(lock) {
        val value = strongMemoryCache.get(key) ?: weakMemoryCache.get(key)

        // Remove unshareable images from the cache when they're returned.
        if (value != null && !value.image.shareable) {
            remove(key)
        }

        return value
    }

    override fun set(key: Key, value: MemoryCache.Value) = synchronized(lock) {
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

    override fun remove(key: Key): Boolean = synchronized(lock) {
        // Do not short circuit. There is a regression test for this.
        val removedStrong = strongMemoryCache.remove(key)
        val removedWeak = weakMemoryCache.remove(key)
        return removedStrong || removedWeak
    }

    override fun trimToSize(size: Long) = synchronized(lock) {
        strongMemoryCache.trimToSize(size)
    }

    override fun clear() = synchronized(lock) {
        strongMemoryCache.clear()
        weakMemoryCache.clear()
    }
}
