package coil3.memory

import coil3.Image
import coil3.memory.MemoryCache.Key
import coil3.memory.MemoryCache.Value
import coil3.util.LruCache

/**
 * An in-memory cache that holds strong references.
 *
 * Values are added to [WeakMemoryCache] when they're evicted from [StrongMemoryCache].
 */
internal interface StrongMemoryCache {

    val size: Long

    var maxSize: Long

    val initialMaxSize: Long

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

    override var maxSize = 0L

    override val initialMaxSize get() = 0L

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
    override val initialMaxSize: Long,
    private val weakMemoryCache: WeakMemoryCache,
) : StrongMemoryCache {

    private val cache = object : LruCache<Key, InternalValue>(initialMaxSize) {
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
        get() = cache.size

    override var maxSize: Long
        get() = cache.maxSize
        set(value) {
            cache.maxSize = value
        }

    override val keys: Set<Key>
        get() = cache.keys

    override fun get(key: Key): Value? {
        return cache[key]?.let { Value(it.image, it.extras) }
    }

    override fun set(
        key: Key,
        image: Image,
        extras: Map<String, Any>,
        size: Long,
    ) {
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

    override fun remove(key: Key): Boolean {
        return cache.remove(key) != null
    }

    override fun clear() {
        cache.clear()
    }

    override fun trimToSize(size: Long) {
        cache.trimToSize(size)
    }

    private class InternalValue(
        val image: Image,
        val extras: Map<String, Any>,
        val size: Long,
    )
}
