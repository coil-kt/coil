package coil.bitmap

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
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
     * Increase the reference count for [bitmap] by one.
     */
    fun increment(bitmap: Bitmap)

    /**
     * Decrease the reference count for [bitmap] by one.
     *
     * If the reference count is now zero, add [bitmap] to the [BitmapPool].
     *
     * @return True if [bitmap] was added to the [BitmapPool] as a result of this decrement operation.
     */
    fun decrement(bitmap: Bitmap): Boolean

    /**
     * Mark [bitmap] as valid/invalid.
     *
     * Once a bitmap has been marked as invalid it cannot be made valid again.
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
        value.count++
        logger?.log(TAG, Log.VERBOSE) { "INCREMENT: [$key, ${value.count}, ${value.isValid}]" }
        cleanUpIfNecessary()
    }

    @Synchronized
    override fun decrement(bitmap: Bitmap): Boolean {
        val key = bitmap.identityHashCode
        val value = getValueOrNull(key, bitmap) ?: run {
            logger?.log(TAG, Log.VERBOSE) { "DECREMENT: [$key, UNKNOWN, UNKNOWN]" }
            return false
        }
        value.count--
        logger?.log(TAG, Log.VERBOSE) { "DECREMENT: [$key, ${value.count}, ${value.isValid}]" }

        // If the bitmap is valid and its count reaches 0, remove it
        // from the WeakMemoryCache and add it to the BitmapPool.
        val removed = value.count <= 0 && value.isValid
        if (removed) {
            values.remove(key)
            weakMemoryCache.remove(bitmap)
            // Add the bitmap to the pool on the next frame.
            MAIN_HANDLER.post { bitmapPool.put(bitmap) }
        }

        cleanUpIfNecessary()
        return removed
    }

    @Synchronized
    override fun setValid(bitmap: Bitmap, isValid: Boolean) {
        val key = bitmap.identityHashCode
        if (isValid) {
            val value = getValueOrNull(key, bitmap)
            if (value == null) {
                values[key] = Value(WeakReference(bitmap), 0, true)
            }
        } else {
            val value = getValue(key, bitmap)
            value.isValid = false
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
            value = Value(WeakReference(bitmap), 0, false)
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
        var isValid: Boolean
    )

    companion object {
        private const val TAG = "RealBitmapReferenceCounter"
        private const val CLEAN_UP_INTERVAL = 50
        private val MAIN_HANDLER = Handler(Looper.getMainLooper())
    }
}
