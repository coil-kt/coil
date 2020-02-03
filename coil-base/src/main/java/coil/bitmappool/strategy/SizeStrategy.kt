package coil.bitmappool.strategy

import android.graphics.Bitmap
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.M
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import coil.collection.GroupedLinkedMap
import coil.util.Utils
import coil.util.getAllocationByteCountCompat
import java.util.TreeMap

/**
 * A strategy for reusing bitmaps that relies on [Bitmap.reconfigure].
 *
 * Keys [Bitmap]s using [Bitmap.getAllocationByteCountCompat].
 * This improves the hit rate over [SizeConfigStrategy], as it allows re-use of bitmaps with different configs.
 *
 * Technically, the APIs for this strategy are available since [KITKAT], however we shouldn't use this strategy until
 * [M] due to framework bugs.
 *
 * Adapted from [Glide](https://github.com/bumptech/glide)'s SizeStrategy.
 * Glide's license information is available [here](https://github.com/bumptech/glide/blob/master/LICENSE).
 */
@RequiresApi(M)
internal class SizeStrategy : BitmapPoolStrategy {

    companion object {
        private const val MAX_SIZE_MULTIPLE = 8

        @Suppress("NOTHING_TO_INLINE")
        private inline fun getBitmapString(size: Int) = "[$size]"
    }

    private val groupedMap = GroupedLinkedMap<Int, Bitmap>()
    private val sortedSizes = TreeMap<Int, Int>()

    override fun put(bitmap: Bitmap) {
        val size = bitmap.getAllocationByteCountCompat()
        groupedMap[size] = bitmap

        val current = sortedSizes[size]
        sortedSizes[size] = if (current == null) 1 else current + 1
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        val size = Utils.calculateAllocationByteCount(width, height, config)
        val bestSize = findBestSize(size)

        // Do a get even if we know we don't have a bitmap so that the key moves to the front in the LRU pool.
        val result = groupedMap[bestSize]
        if (result != null) {
            // Decrement must be called before reconfigure.
            decrementBitmapOfSize(bestSize, result)
            result.reconfigure(width, height, config)
        }
        return result
    }

    private fun findBestSize(size: Int): Int {
        val possibleSize: Int? = sortedSizes.ceilingKey(size)
        return if (possibleSize != null && possibleSize <= size * MAX_SIZE_MULTIPLE) {
            possibleSize
        } else {
            size
        }
    }

    override fun removeLast(): Bitmap? {
        val removed = groupedMap.removeLast()
        if (removed != null) {
            decrementBitmapOfSize(removed.getAllocationByteCountCompat(), removed)
        }
        return removed
    }

    private fun decrementBitmapOfSize(size: Int, removed: Bitmap) {
        val current = sortedSizes[size] ?: run {
            throw NullPointerException("Tried to decrement empty size, size: $size, " +
                "removed: ${logBitmap(removed)}, this: $this")
        }

        if (current == 1) {
            sortedSizes.remove(size)
        } else {
            sortedSizes[size] = current - 1
        }
    }

    override fun logBitmap(bitmap: Bitmap): String {
        return getBitmapString(bitmap.getAllocationByteCountCompat())
    }

    override fun logBitmap(@Px width: Int, @Px height: Int, config: Bitmap.Config): String {
        val size = Utils.calculateAllocationByteCount(width, height, config)
        return getBitmapString(size)
    }

    override fun toString(): String {
        return "SizeStrategy: groupedMap=$groupedMap, sortedSizes=($sortedSizes)"
    }
}
