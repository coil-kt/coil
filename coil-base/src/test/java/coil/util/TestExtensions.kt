package coil.util

import android.graphics.Bitmap
import androidx.collection.forEach
import coil.bitmap.RealBitmapReferenceCounter
import coil.memory.RealWeakMemoryCache

/** Return the current reference count for [bitmap]. */
internal fun RealBitmapReferenceCounter.count(bitmap: Bitmap): Int {
    return values[bitmap.identityHashCode]?.takeIf { it.bitmap.get() === bitmap }?.count ?: 0
}

/** Return true if [bitmap]'s reference count is valid. */
internal fun RealBitmapReferenceCounter.isValid(bitmap: Bitmap): Boolean {
    return values[bitmap.identityHashCode]?.takeIf { it.bitmap.get() === bitmap }?.isValid == true
}

/**
 * Clears [bitmap]'s weak reference without removing its entry from [RealWeakMemoryCache.cache].
 * This simulates garbage collection.
 */
internal fun RealBitmapReferenceCounter.clear(bitmap: Bitmap) {
    values.forEach { _, value ->
        if (value.bitmap.get() === bitmap) {
            value.bitmap.clear()
            return
        }
    }
}

/**
 * Clears [bitmap]'s weak reference without removing its entry from [RealWeakMemoryCache.cache].
 * This simulates garbage collection.
 */
internal fun RealWeakMemoryCache.clear(bitmap: Bitmap) {
    cache.values.forEach { values ->
        values.forEachIndices { value ->
            if (value.bitmap.get() === bitmap) {
                value.bitmap.clear()
                return
            }
        }
    }
}

/** Return the number of values currently in the cache. */
internal fun RealWeakMemoryCache.count(): Int = cache.count()
