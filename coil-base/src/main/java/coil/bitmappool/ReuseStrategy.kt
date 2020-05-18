package coil.bitmappool

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import coil.collection.LinkedMultimap
import coil.util.Utils
import coil.util.allocationByteCountCompat
import java.util.TreeMap

/** The [Bitmap] reuse algorithm used by [RealBitmapPool]. */
internal interface ReuseStrategy {

    companion object {
        operator fun invoke(): ReuseStrategy {
            return when {
                SDK_INT >= 19 -> SizeStrategy()
                else -> AttributeStrategy()
            }
        }
    }

    /** Add [bitmap] to the LRU cache. */
    fun put(bitmap: Bitmap)

    /** Return a reusable bitmap matching [width], [height], and [config]. Return null if there is no match. */
    fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?

    /** Remove the least recently used bitmap from the cache and return it. */
    fun removeLast(): Bitmap?

    /** Return a string representation of [bitmap]. */
    fun stringify(bitmap: Bitmap): String

    /** Return a string representation of a [Bitmap] matching [width], [height], and [config]. */
    fun stringify(@Px width: Int, @Px height: Int, config: Bitmap.Config): String
}

/** A strategy for reusing bitmaps that relies on [Bitmap.reconfigure]. */
@VisibleForTesting
@RequiresApi(19)
internal class SizeStrategy : ReuseStrategy {

    companion object {
        private const val MAX_SIZE_MULTIPLE = 8
    }

    private val entries = LinkedMultimap<Int, Bitmap>()
    private val sizes = TreeMap<Int, Int>()

    override fun put(bitmap: Bitmap) {
        val size = bitmap.allocationByteCountCompat
        entries[size] = bitmap
        sizes[size] = sizes.getOrElse(size) { 0 } + 1
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        val size = Utils.calculateAllocationByteCount(width, height, config)
        val bestSize = findBestSize(size) ?: size

        // Do a get even if we know we don't have a bitmap so that the key becomes the head of the linked list.
        val result = entries[bestSize]
        if (result != null) {
            // Decrement must be called before reconfigure.
            decrementBitmapOfSize(bestSize, result)
            result.reconfigure(width, height, config)
        }
        return result
    }

    /** Return the least key greater than [size]. If no such key exists, return null. */
    private fun findBestSize(size: Int): Int? {
        return sizes.ceilingKey(size)?.takeIf { it <= size * MAX_SIZE_MULTIPLE }
    }

    override fun removeLast(): Bitmap? {
        val removed = entries.removeLast()
        if (removed != null) {
            decrementBitmapOfSize(removed.allocationByteCountCompat, removed)
        }
        return removed
    }

    private fun decrementBitmapOfSize(size: Int, removed: Bitmap) {
        val current = checkNotNull(sizes[size]) {
            "Tried to decrement empty size, size: $size, removed: ${stringify(removed)}, this: $this"
        }

        if (current > 1) {
            sizes[size] = current - 1
        } else {
            sizes.remove(size)
        }
    }

    override fun stringify(bitmap: Bitmap) = "[${bitmap.allocationByteCountCompat}]"

    override fun stringify(@Px width: Int, @Px height: Int, config: Bitmap.Config): String {
        return "[${Utils.calculateAllocationByteCount(width, height, config)}]"
    }

    override fun toString() = "SizeStrategy: entries=$entries, sizes=$sizes"
}

/** A strategy for reusing bitmaps that requires bitmaps' width, height, and config to match exactly. */
@VisibleForTesting
internal class AttributeStrategy : ReuseStrategy {

    private val entries = LinkedMultimap<Key, Bitmap>()

    override fun put(bitmap: Bitmap) {
        entries[Key(bitmap.width, bitmap.height, bitmap.config)] = bitmap
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        return entries[Key(width, height, config)]
    }

    override fun removeLast(): Bitmap? {
        return entries.removeLast()
    }

    override fun stringify(bitmap: Bitmap) = stringify(bitmap.width, bitmap.height, bitmap.config)

    override fun stringify(width: Int, height: Int, config: Bitmap.Config) = "[$width x $height], $config"

    override fun toString() = "AttributeStrategy: entries=$entries"

    private data class Key(
        @Px val width: Int,
        @Px val height: Int,
        val config: Bitmap.Config
    )
}
