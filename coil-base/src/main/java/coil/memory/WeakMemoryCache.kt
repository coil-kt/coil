@file:Suppress("NOTHING_TO_INLINE")

package coil.memory

import android.graphics.Bitmap
import androidx.collection.SparseArrayCompat
import androidx.collection.set
import coil.memory.MemoryCache.Value
import coil.util.identityHashCode
import java.lang.ref.WeakReference

/**
 * An in-memory cache that holds weak references to [Bitmap]s.
 *
 * This is used as a secondary caching layer for [MemoryCache]. [MemoryCache] holds strong references to its bitmaps.
 * Bitmaps are added to this cache when they're removed from [MemoryCache].
 */
internal class WeakMemoryCache {

    companion object {
        private const val CLEAN_UP_INTERVAL = 10
    }

    private val cache = LinkedHashMap<String, InternalValue>()
    private val hashCodeToKey = SparseArrayCompat<String>()

    @Volatile private var operationsSinceCleanUp = 0

    fun get(key: String): Value? = cache[key]

    fun set(key: String, bitmap: Bitmap, isSampled: Boolean) {
        hashCodeToKey[bitmap.identityHashCode] = key
        cache[key] = InternalValue(WeakReference(bitmap), isSampled)
        cleanUpIfNecessary()
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun remove(bitmap: Bitmap) {
        hashCodeToKey[bitmap.identityHashCode]?.let(::remove)
    }

    fun clear() {
        hashCodeToKey.clear()
        cache.clear()
    }

    /** Remove any dereferenced bitmaps from the cache. */
    private fun cleanUpIfNecessary() {
        if (operationsSinceCleanUp++ < CLEAN_UP_INTERVAL) {
            return
        }

        synchronized(this) {
            val iterator = cache.values.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().bitmap == null) {
                    iterator.remove()
                    // TODO: Clear the entry in hashCodeToKey.
                }
            }
        }
    }

    private data class InternalValue(
        val reference: WeakReference<Bitmap>,
        override val isSampled: Boolean
    ) : Value {
        override val bitmap: Bitmap?
            get() = reference.get()
    }
}
