package coil.util

import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import coil.bitmap.BitmapReferenceCounter
import coil.memory.RealWeakMemoryCache

/** Return the current reference count for [bitmap]. */
internal fun BitmapReferenceCounter.count(bitmap: Bitmap): Int {
    return counts[bitmap.identityHashCode]
}

/** Return true if [bitmap]'s reference count is invalid. */
internal fun BitmapReferenceCounter.isInvalid(bitmap: Bitmap): Boolean {
    return bitmap.identityHashCode in invalidKeys
}

/**
 * Clears [bitmap]'s weak reference without removing its entry from [RealWeakMemoryCache.cache].
 * This simulates garbage collection.
 */
@VisibleForTesting
internal fun RealWeakMemoryCache.clear(bitmap: Bitmap) {
    cache.values.forEach { values ->
        values.forEachIndices { value ->
            if (value.reference.get() == bitmap) {
                value.reference.clear()
                return
            }
        }
    }
}

/** Return the number of values currently in the cache. */
@VisibleForTesting
internal fun RealWeakMemoryCache.count(): Int = cache.count()
