package coil3.memory

import coil3.Image
import coil3.memory.MemoryCache.Key
import coil3.memory.MemoryCache.Value
import coil3.util.LruCache
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * An in-memory cache that holds strong references.
 *
 * Values are added to [WeakMemoryCache] when they're evicted from [StrongMemoryCache].
 */
internal interface StrongMemoryCache {

    val size: Long

    val maxSize: Long

    val keys: Set<Key>

    fun get(key: Key): Value?

    fun set(key: Key, image: Image, extras: Map<String, Any>, size: Long)

    fun remove(key: Key): Boolean

    fun trimToSize(size: Long)

    fun clear()
}

internal class EmptyStrongMemoryCache(
    private val weakMemoryCache: WeakMemoryCache,
) : StrongMemoryCache {

    override val size get() = 0L

    override val maxSize get() = 0L

    override val keys get() = emptySet<Key>()

    override fun get(key: Key): Value? = null

    override fun set(key: Key, image: Image, extras: Map<String, Any>, size: Long) {
        weakMemoryCache.set(key, image, extras, size)
    }

    override fun remove(key: Key) = false

    override fun trimToSize(size: Long) {}

    override fun clear() {}
}

internal class RealStrongMemoryCache(
    maxSize: Long,
    private val weakMemoryCache: WeakMemoryCache,
) : StrongMemoryCache {

    private val lock = SynchronizedObject()
    private val cache = object : LruCache<Key, InternalValue>(maxSize) {
        override fun sizeOf(
            key: Key,
            value: InternalValue,
        ) = value.size

        override fun entryRemoved(
            key: Key,
            oldValue: InternalValue,
            newValue: InternalValue?,
        ) = weakMemoryCache.set(key, oldValue.image, oldValue.extras, oldValue.size)
    }

    override val size: Long
        get() = synchronized(lock) { cache.size }

    override val maxSize: Long
        get() = synchronized(lock) { cache.maxSize }

    override val keys: Set<Key>
        get() = synchronized(lock) { cache.keys }

    override fun get(key: Key): Value? = synchronized(lock) {
        return cache[key]?.let { Value(it.image, it.extras) }
    }

    override fun set(
        key: Key,
        image: Image,
        extras: Map<String, Any>,
        size: Long,
    ): Unit = synchronized(lock) {
        if (size <= maxSize) {
            cache.put(key, InternalValue(image, extras, size))
        } else {
            // If the value is too big for the cache, don't attempt to store it as doing
            // so will cause the cache to be cleared. Instead, evict an existing element
            // with the same key if it exists and add the value to the weak memory cache.
            cache.remove(key)
            weakMemoryCache.set(key, image, extras, size)
        }
    }

    override fun remove(key: Key): Boolean = synchronized(lock) {
        return cache.remove(key) != null
    }

    override fun clear() = synchronized(lock) {
        cache.clear()
    }

    override fun trimToSize(size: Long) = synchronized(lock) {
        cache.trimToSize(size)
    }

    private class InternalValue(
        val image: Image,
        val extras: Map<String, Any>,
        val size: Long,
    )
}
