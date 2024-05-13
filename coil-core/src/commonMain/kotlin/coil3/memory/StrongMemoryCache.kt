package coil3.memory

import androidx.collection.LruCache
import coil3.Image
import coil3.memory.MemoryCache.Key
import coil3.memory.MemoryCache.Value
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * An in-memory cache that holds strong references.
 *
 * Values are added to [WeakMemoryCache] when they're evicted from [StrongMemoryCache].
 */
internal interface StrongMemoryCache {

    val size: Int

    val maxSize: Int

    val keys: Set<Key>

    fun get(key: Key): Value?

    fun set(key: Key, image: Image, extras: Map<String, Any>, size: Int)

    fun remove(key: Key): Boolean

    fun trimToSize(size: Int)

    fun clear()
}

internal class EmptyStrongMemoryCache(
    private val weakMemoryCache: WeakMemoryCache,
) : StrongMemoryCache {

    override val size get() = 0

    override val maxSize get() = 0

    override val keys get() = emptySet<Key>()

    override fun get(key: Key): Value? = null

    override fun set(key: Key, image: Image, extras: Map<String, Any>, size: Int) {
        weakMemoryCache.set(key, image, extras, size.toLong())
    }

    override fun remove(key: Key) = false

    override fun trimToSize(size: Int) {}

    override fun clear() {}
}

internal class RealStrongMemoryCache(
    maxSize: Int,
    private val weakMemoryCache: WeakMemoryCache,
) : StrongMemoryCache {

    private val lock = SynchronizedObject()
    private val cache = object : LruCache<Key, InternalValue>(maxSize) {
        override fun sizeOf(
            key: Key,
            value: InternalValue,
        ) = value.size

        override fun entryRemoved(
            evicted: Boolean,
            key: Key,
            oldValue: InternalValue,
            newValue: InternalValue?,
        ) = weakMemoryCache.set(key, oldValue.image, oldValue.extras, oldValue.size.toLong())
    }

    override val size: Int
        get() = synchronized(lock) { cache.size() }

    override val maxSize: Int
        get() = synchronized(lock) { cache.maxSize() }

    override val keys: Set<Key>
        get() = synchronized(lock) { cache.snapshot().keys }

    override fun get(key: Key): Value? = synchronized(lock) {
        return cache[key]?.let { Value(it.image, it.extras) }
    }

    override fun set(
        key: Key,
        image: Image,
        extras: Map<String, Any>,
        size: Int,
    ): Unit = synchronized(lock) {
        if (size <= maxSize) {
            cache.put(key, InternalValue(image, extras, size))
        } else {
            // If the value is too big for the cache, don't attempt to store it as doing
            // so will cause the cache to be cleared. Instead, evict an existing element
            // with the same key if it exists and add the value to the weak memory cache.
            cache.remove(key)
            weakMemoryCache.set(key, image, extras, size.toLong())
        }
    }

    override fun remove(key: Key): Boolean = synchronized(lock) {
        return cache.remove(key) != null
    }

    override fun clear() = synchronized(lock) {
        cache.evictAll()
    }

    override fun trimToSize(size: Int) = synchronized(lock) {
        cache.trimToSize(size)
    }

    private class InternalValue(
        val image: Image,
        val extras: Map<String, Any>,
        val size: Int,
    )
}
