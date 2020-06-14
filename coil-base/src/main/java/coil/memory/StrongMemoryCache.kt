package coil.memory

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import androidx.collection.LruCache
import coil.annotation.ExperimentalCoilApi
import coil.memory.MemoryCache.Key
import coil.memory.RealMemoryCache.Value
import coil.util.allocationByteCountCompat

/** An in-memory cache that holds strong references [Bitmap]s. */
@OptIn(ExperimentalCoilApi::class)
internal interface StrongMemoryCache {

    companion object {
        operator fun invoke(
            weakMemoryCache: WeakMemoryCache,
            referenceCounter: BitmapReferenceCounter,
            maxSize: Int
        ): StrongMemoryCache {
            return when {
                maxSize > 0 -> RealStrongMemoryCache(weakMemoryCache, referenceCounter, maxSize)
                weakMemoryCache is RealWeakMemoryCache -> ForwardingStrongMemoryCache(weakMemoryCache)
                else -> EmptyStrongMemoryCache
            }
        }
    }

    /** The current size of the memory cache in bytes. */
    val size: Int

    /** The maximum size of the memory cache in bytes. */
    val maxSize: Int

    /** Get the value associated with [key]. */
    fun get(key: Key): Value?

    /** Set the value associated with [key]. */
    fun set(key: Key, bitmap: Bitmap, isSampled: Boolean)

    /** Remove the value referenced by [key] from this cache. */
    fun remove(key: Key): Boolean

    /** Remove all values from this cache. */
    fun clearMemory()

    /** @see ComponentCallbacks2.onTrimMemory */
    fun trimMemory(level: Int)
}

/** A [StrongMemoryCache] implementation that caches nothing. */
@OptIn(ExperimentalCoilApi::class)
private object EmptyStrongMemoryCache : StrongMemoryCache {

    override val size get() = 0

    override val maxSize get() = 0

    override fun get(key: Key): Value? = null

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean) {}

    override fun remove(key: Key) = false

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [StrongMemoryCache] implementation that caches nothing and delegates all [set] operations to a [weakMemoryCache]. */
@OptIn(ExperimentalCoilApi::class)
private class ForwardingStrongMemoryCache(
    private val weakMemoryCache: WeakMemoryCache
) : StrongMemoryCache {

    override val size get() = 0

    override val maxSize get() = 0

    override fun get(key: Key): Value? = null

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean) {
        weakMemoryCache.set(key, bitmap, isSampled, bitmap.allocationByteCountCompat)
    }

    override fun remove(key: Key) = false

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [StrongMemoryCache] implementation backed by an [LruCache]. */
@OptIn(ExperimentalCoilApi::class)
private class RealStrongMemoryCache(
    private val weakMemoryCache: WeakMemoryCache,
    private val referenceCounter: BitmapReferenceCounter,
    maxSize: Int
) : StrongMemoryCache {

    private val cache = object : LruCache<Key, InternalValue>(maxSize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: Key,
            oldValue: InternalValue,
            newValue: InternalValue?
        ) {
            val isPooled = referenceCounter.decrement(oldValue.bitmap)
            if (!isPooled) {
                // Add the bitmap to the WeakMemoryCache if it wasn't just added to the BitmapPool.
                weakMemoryCache.set(key, oldValue.bitmap, oldValue.isSampled, oldValue.size)
            }
        }

        override fun sizeOf(key: Key, value: InternalValue) = value.size
    }

    override val size get() = cache.size()

    override val maxSize get() = cache.maxSize()

    override fun get(key: Key) = cache.get(key)

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean) = synchronized(cache) {
        // If the bitmap is too big for the cache, don't even attempt to store it. Doing so will cause
        // the cache to be cleared. Instead just evict an existing element with the same key if it exists.
        val size = bitmap.allocationByteCountCompat
        if (size > maxSize) {
            val previous = cache.remove(key)
            if (previous == null) {
                // If previous != null, the value was already added to the weak memory cache in LruCache.entryRemoved.
                weakMemoryCache.set(key, bitmap, isSampled, size)
            }
            return@synchronized
        }

        referenceCounter.increment(bitmap)
        cache.put(key, InternalValue(bitmap, isSampled, size))
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
        override val bitmap: Bitmap,
        override val isSampled: Boolean,
        val size: Int
    ) : Value
}
