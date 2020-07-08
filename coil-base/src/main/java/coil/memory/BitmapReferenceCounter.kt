package coil.memory

import android.graphics.Bitmap
import android.util.Log
import android.util.SparseIntArray
import androidx.annotation.VisibleForTesting
import androidx.core.util.set
import coil.bitmappool.BitmapPool
import coil.collection.SparseIntArraySet
import coil.collection.plusAssign
import coil.util.Logger
import coil.util.identityHashCode
import coil.util.log
import java.lang.ref.WeakReference

/**
 * Count references to [Bitmap]s. Add bitmaps to the [bitmapPool] when they're no longer referenced.
 *
 * This class uses [System.identityHashCode] to determine bitmap identity as it provides a "unique-enough" key
 * for a [Bitmap] and it allows us to avoid using [WeakReference]s.
 */
internal class BitmapReferenceCounter(
    private val weakMemoryCache: WeakMemoryCache,
    private val bitmapPool: BitmapPool,
    private val logger: Logger?
) {

    @VisibleForTesting internal val counts = SparseIntArray()
    @VisibleForTesting internal val invalidKeys = SparseIntArraySet()

    /**
     * Increase the reference count for this [Bitmap] by one.
     */
    @Synchronized
    fun increment(bitmap: Bitmap) {
        val key = bitmap.identityHashCode
        val count = counts[key]
        val newCount = count + 1
        counts[key] = newCount
        logger?.log(TAG, Log.VERBOSE) { "INCREMENT: [$key, $newCount]" }
    }

    /**
     * Decrease the reference count for this [Bitmap] by one.
     *
     * If the reference count is now zero, add the [Bitmap] to [bitmapPool].
     *
     * @return True if [bitmap] was added to [bitmapPool] as a result of this decrement operation.
     */
    @Synchronized
    fun decrement(bitmap: Bitmap): Boolean {
        val key = bitmap.identityHashCode
        val count = counts[key]
        val newCount = count - 1
        counts[key] = newCount
        logger?.log(TAG, Log.VERBOSE) { "DECREMENT: [$key, $newCount]" }

        if (newCount <= 0) {
            counts.delete(key)
            val isValid = !invalidKeys.remove(key)
            if (isValid) {
                // Remove the bitmap from the WeakMemoryCache and add it to the BitmapPool.
                weakMemoryCache.remove(bitmap)
                bitmapPool.put(bitmap)
                return true
            }
        }

        return false
    }

    /**
     * Mark this bitmap as invalid so it is not returned to the bitmap pool
     * when it is no longer referenced.
     */
    @Synchronized
    fun invalidate(bitmap: Bitmap) {
        invalidKeys += bitmap.identityHashCode
    }

    companion object {
        private const val TAG = "BitmapReferenceCounter"
    }
}
