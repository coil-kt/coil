package coil.bitmap

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.collection.SparseArrayCompat
import androidx.collection.set
import androidx.collection.size
import coil.memory.WeakMemoryCache
import coil.util.Logger
import coil.util.forEachIndices
import coil.util.identityHashCode
import coil.util.log
import java.lang.ref.WeakReference

/**
 * Count references to [Bitmap]s and add them to a [BitmapPool] when they're no longer referenced.
 */
internal interface BitmapReferenceCounter {

    /**
     * Increase the reference count for this [Bitmap] by one.
     */
    fun increment(bitmap: Bitmap)

    /**
     * Decrease the reference count for this [Bitmap] by one.
     *
     * If the reference count is now zero, add the [Bitmap] to the [BitmapPool].
     *
     * @return True if [bitmap] was added to the [BitmapPool] as a result of this decrement operation.
     */
    fun decrement(bitmap: Bitmap): Boolean

    /**
     * Mark [bitmap] as valid/invalid.
     *
     * Only valid bitmaps are added to the [BitmapPool] when its reference count reaches zero.
     */
    fun setValid(bitmap: Bitmap, isValid: Boolean)
}

internal object EmptyBitmapReferenceCounter : BitmapReferenceCounter {

    override fun increment(bitmap: Bitmap) {}

    override fun decrement(bitmap: Bitmap) = false

    override fun setValid(bitmap: Bitmap, isValid: Boolean) {}
}

internal class RealBitmapReferenceCounter(
    private val weakMemoryCache: WeakMemoryCache,
    private val bitmapPool: BitmapPool,
    private val logger: Logger?
) : BitmapReferenceCounter {

    @VisibleForTesting internal val values = SparseArrayCompat<Value>()
    @VisibleForTesting internal var operationsSinceCleanUp = 0

    @Synchronized
    override fun increment(bitmap: Bitmap) {
        val key = bitmap.identityHashCode
        val value = getValue(key, bitmap)
        val newCount = value.count + 1
        value.count = newCount
        logger?.log(TAG, Log.VERBOSE) { "INCREMENT: [$key, $newCount]" }

        cleanUpIfNecessary()
    }

    @Synchronized
    override fun decrement(bitmap: Bitmap): Boolean {
        val key = bitmap.identityHashCode
        val value = getValueOrNull(key, bitmap) ?: return false
        val newCount = value.count - 1
        value.count = newCount
        logger?.log(TAG, Log.VERBOSE) { "DECREMENT: [$key, $newCount]" }

        // If the bitmap is valid and its count reaches 0, remove it from the
        // WeakMemoryCache and add it to the BitmapPool.
        val removed = newCount <= 0 && value.state == STATE_VALID
        if (removed) {
            values.remove(key)
            weakMemoryCache.remove(bitmap)
            bitmapPool.put(bitmap)
        }

        cleanUpIfNecessary()
        return removed
    }

    @Synchronized
    override fun setValid(bitmap: Bitmap, isValid: Boolean) {
        val key = bitmap.identityHashCode
        val value = getValue(key, bitmap)
        if (value.state != STATE_INVALID) {
            value.state = if (isValid) STATE_VALID else STATE_INVALID
        }
        cleanUpIfNecessary()
    }

    private fun cleanUpIfNecessary() {
        if (operationsSinceCleanUp++ >= CLEAN_UP_INTERVAL) {
            cleanUp()
        }
    }

    @VisibleForTesting
    internal fun cleanUp() {
        val toRemove = arrayListOf<Int>()
        for (index in 0 until values.size) {
            val value = values.valueAt(index)
            if (value.bitmap.get() == null) {
                // Don't remove the values while iterating over the loop so
                // we don't trigger SparseArray's internal GC for each removal.
                toRemove += index
            }
        }
        toRemove.forEachIndices(values::removeAt)
    }

    private fun getValue(key: Int, bitmap: Bitmap): Value {
        var value = getValueOrNull(key, bitmap)
        if (value == null) {
            value = Value(WeakReference(bitmap), 0, STATE_UNSET)
            values[key] = value
        }
        return value
    }

    private fun getValueOrNull(key: Int, bitmap: Bitmap): Value? {
        return values[key]?.takeIf { it.bitmap.get() === bitmap }
    }

    @VisibleForTesting
    internal class Value(
        val bitmap: WeakReference<Bitmap>,
        var count: Int,
        var state: Int
    )

    companion object {
        private const val TAG = "RealBitmapReferenceCounter"
        private const val CLEAN_UP_INTERVAL = 50

        internal const val STATE_UNSET = 0
        internal const val STATE_VALID = 1
        internal const val STATE_INVALID = 2
    }
}
