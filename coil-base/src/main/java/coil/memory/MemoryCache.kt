package coil.memory

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import android.util.Log
import androidx.collection.LruCache
import coil.util.getAllocationByteCountCompat
import coil.util.log

/**
 * An LRU cache for [Bitmap]s that were recently loaded into memory.
 */
internal class MemoryCache(
    private val referenceCounter: BitmapReferenceCounter,
    maxSize: Int
) {

    companion object {
        private const val TAG = "MemoryCache"
    }

    private val cache: LruCache<String, Value>? = createMemoryCache(maxSize)

    private fun createMemoryCache(maxSize: Int): LruCache<String, Value>? {
        return if (maxSize > 0) {
            object : LruCache<String, Value>(maxSize) {
                override fun entryRemoved(
                    evicted: Boolean,
                    key: String,
                    oldValue: Value,
                    newValue: Value?
                ) = referenceCounter.decrement(oldValue.bitmap)

                override fun sizeOf(key: String, value: Value) = value.size
            }
        } else {
            null
        }
    }

    operator fun get(key: String): Value? = cache?.get(key)

    fun set(key: String, value: Bitmap, isSampled: Boolean) {
        // If the bitmap is too big for the cache, don't even attempt to store it. Doing so will cause
        // the cache to be cleared. Instead just evict an existing element with the same key if it exists.
        val size = value.getAllocationByteCountCompat()
        if (size > maxSize()) {
            cache?.remove(key)
            return
        }

        referenceCounter.increment(value)
        cache?.put(key, Value(value, isSampled, size))
    }

    fun size(): Int = cache?.size() ?: 0

    fun maxSize(): Int = cache?.maxSize() ?: 0

    fun clearMemory() {
        log(TAG, Log.DEBUG) { "clearMemory" }
        cache?.trimToSize(-1)
    }

    fun trimMemory(level: Int) {
        log(TAG, Log.DEBUG) { "trimMemory, level=$level" }
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            cache?.trimToSize(size() / 2)
        }
    }

    data class Value(
        val bitmap: Bitmap,
        val isSampled: Boolean,
        val size: Int
    )
}
