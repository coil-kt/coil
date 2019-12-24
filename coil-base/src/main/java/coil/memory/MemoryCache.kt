package coil.memory

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import android.util.Log
import androidx.collection.LruCache
import coil.memory.MemoryCache.Value
import coil.util.getAllocationByteCountCompat
import coil.util.log

/** A memory cache for [Bitmap]s. */
internal interface MemoryCache {

    companion object {
        operator fun invoke(
            referenceCounter: BitmapReferenceCounter,
            maxSize: Int
        ): MemoryCache {
            return if (maxSize > 0) {
                RealMemoryCache(referenceCounter, maxSize)
            } else {
                EmptyMemoryCache
            }
        }
    }

    fun get(key: String): Value?

    fun set(key: String, value: Bitmap, isSampled: Boolean)

    fun size(): Int

    fun maxSize(): Int

    fun clearMemory()

    fun trimMemory(level: Int)

    data class Value(
        val bitmap: Bitmap,
        val isSampled: Boolean,
        val size: Int
    )
}

/** A [MemoryCache] implementation that caches nothing. */
private object EmptyMemoryCache : MemoryCache {

    override fun get(key: String): Value? = null

    override fun set(key: String, value: Bitmap, isSampled: Boolean) {}

    override fun size(): Int = 0

    override fun maxSize(): Int = 0

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [MemoryCache] implementation backed by an [LruCache]. */
private class RealMemoryCache(
    private val referenceCounter: BitmapReferenceCounter,
    maxSize: Int
) : MemoryCache {

    companion object {
        private const val TAG = "RealMemoryCache"
    }

    private val cache = object : LruCache<String, Value>(maxSize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: Value,
            newValue: Value?
        ) = referenceCounter.decrement(oldValue.bitmap)

        override fun sizeOf(key: String, value: Value) = value.size
    }

    override fun get(key: String): Value? = cache.get(key)

    override fun set(key: String, value: Bitmap, isSampled: Boolean) {
        // If the bitmap is too big for the cache, don't even attempt to store it. Doing so will cause
        // the cache to be cleared. Instead just evict an existing element with the same key if it exists.
        val size = value.getAllocationByteCountCompat()
        if (size > maxSize()) {
            cache.remove(key)
            return
        }

        referenceCounter.increment(value)
        cache.put(key, Value(value, isSampled, size))
    }

    override fun size(): Int = cache.size()

    override fun maxSize(): Int = cache.maxSize()

    override fun clearMemory() {
        log(TAG, Log.DEBUG) { "clearMemory" }
        cache.trimToSize(-1)
    }

    override fun trimMemory(level: Int) {
        log(TAG, Log.DEBUG) { "trimMemory, level=$level" }
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(size() / 2)
        }
    }
}
