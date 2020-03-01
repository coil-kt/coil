package coil.util

import android.graphics.Bitmap
import androidx.annotation.VisibleForTesting
import coil.memory.BitmapReferenceCounter
import coil.memory.WeakMemoryCache

internal fun BitmapReferenceCounter.count(bitmap: Bitmap): Int {
    return counts[bitmap.identityHashCode]
}

internal fun BitmapReferenceCounter.isInvalid(bitmap: Bitmap): Boolean {
    return bitmap.identityHashCode in invalidKeys
}


/** Clears [bitmap]'s weak reference without removing it from [cache]. This simulates garbage collection. */
@VisibleForTesting
internal fun WeakMemoryCache.clear(bitmap: Bitmap) {
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
internal fun WeakMemoryCache.count() = cache.count()
