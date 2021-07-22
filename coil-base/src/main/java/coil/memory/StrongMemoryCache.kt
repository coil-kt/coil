package coil.memory

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import androidx.collection.LruCache
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import coil.util.allocationByteCountCompat

/** An in-memory cache that holds strong references [Bitmap]s. */
internal interface StrongMemoryCache {

    val size: Int

    val maxSize: Int

    val keys: Set<Key>

    fun get(key: Key): Value?

    fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>)

    fun remove(key: Key): Boolean

    fun clearMemory()

    fun trimMemory(level: Int)
}

/** A [StrongMemoryCache] implementation that caches nothing and only delegates [set]s to a [WeakMemoryCache]. */
internal class EmptyStrongMemoryCache(
    private val weakMemoryCache: WeakMemoryCache
) : StrongMemoryCache {

    override val size get() = 0

    override val maxSize get() = 0

    override val keys get() = emptySet<Key>()

    override fun get(key: Key): Value? = null

    override fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>) {
        weakMemoryCache.set(key, bitmap, extras, bitmap.allocationByteCountCompat)
    }

    override fun remove(key: Key) = false

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [StrongMemoryCache] implementation backed by an [LruCache]. */
internal class RealStrongMemoryCache(
    maxSize: Int,
    private val weakMemoryCache: WeakMemoryCache
) : StrongMemoryCache {

    private val cache = object : LruCache<Key, InternalValue>(maxSize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: Key,
            oldValue: InternalValue,
            newValue: InternalValue?
        ) {
            weakMemoryCache.set(key, oldValue.bitmap, oldValue.extras, oldValue.size)
        }

        override fun sizeOf(key: Key, value: InternalValue) = value.size
    }

    override val size get() = cache.size()

    override val maxSize get() = cache.maxSize()

    override val keys get() = cache.snapshot().keys

    override fun get(key: Key): Value? {
        return cache.get(key)?.let { Value(it.bitmap, it.extras) }
    }

    override fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>) {
        val size = bitmap.allocationByteCountCompat
        if (size <= maxSize) {
            cache.put(key, InternalValue(bitmap, extras, size))
        } else {
            // If the bitmap is too big for the cache, don't attempt to store it as doing
            // so will cause the cache to be cleared. Instead, evict an existing element
            // with the same key if it exists and add the bitmap to the weak memory cache.
            cache.remove(key)
            weakMemoryCache.set(key, bitmap, extras, size)
        }
    }

    override fun remove(key: Key): Boolean {
        return cache.remove(key) != null
    }

    override fun clearMemory() {
        cache.trimToSize(-1)
    }

    override fun trimMemory(level: Int) {
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(size / 2)
        }
    }

    private class InternalValue(
        val bitmap: Bitmap,
        val extras: Map<String, Any>,
        val size: Int
    )
}
