@file:Suppress("NOTHING_TO_INLINE")

package coil.memory

import android.graphics.Bitmap
import android.util.Log
import android.util.SparseIntArray
import androidx.annotation.VisibleForTesting
import androidx.core.util.set
import coil.bitmappool.BitmapPool
import coil.collection.SparseIntArraySet
import coil.extension.plusAssign
import coil.util.log
import java.lang.ref.WeakReference

/**
 * Count references to [Bitmap]s. Add bitmaps to the [bitmapPool] when they're no longer referenced.
 *
 * NOTE: This class is not thread safe. In practice, it will only be called from the main thread.
 */
internal class BitmapReferenceCounter(private val bitmapPool: BitmapPool) {

    companion object {
        private const val TAG = "BitmapReferenceCounter"
    }

    private val counts = SparseIntArray()
    private val invalidKeys = SparseIntArraySet()

    /**
     * Increase the reference count for this [Bitmap] by one.
     */
    fun increment(bitmap: Bitmap) {
        val key = bitmap.key()
        val count = counts[key]
        val newCount = count + 1
        counts[key] = newCount
        log(TAG, Log.VERBOSE) { "INCREMENT: [$key, $newCount]" }
    }

    /**
     * Decrease the reference count for this [Bitmap] by one.
     *
     * If the reference count is now zero, add the [Bitmap] to [bitmapPool].
     */
    fun decrement(bitmap: Bitmap) {
        val key = bitmap.key()
        val count = counts[key]
        val newCount = count - 1
        counts[key] = newCount
        log(TAG, Log.VERBOSE) { "DECREMENT: [$key, $newCount]" }

        if (newCount <= 0) {
            counts.delete(key)
            val isValid = !invalidKeys.remove(key)
            if (isValid) {
                bitmapPool.put(bitmap)
            }
        }
    }

    /**
     * Mark this Bitmap as invalid so it is not returned to the Bitmap pool
     * when it is no longer referenced.
     */
    fun invalidate(bitmap: Bitmap) {
        invalidKeys += bitmap.key()
    }

    @VisibleForTesting
    fun count(bitmap: Bitmap): Int {
        return counts[bitmap.key()]
    }

    @VisibleForTesting
    fun invalid(bitmap: Bitmap): Boolean {
        return invalidKeys.contains(bitmap.key())
    }

    /**
     * [System.identityHashCode] provides a "unique-enough" key for a [Bitmap],
     * which allows us to avoid using [WeakReference]s.
     */
    private inline fun Bitmap.key(): Int {
        return System.identityHashCode(this)
    }
}
