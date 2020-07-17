package coil.bitmap

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
internal interface BitmapPoolStrategy {

    companion object {
        operator fun invoke(): BitmapPoolStrategy {
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

    /** Return a string representation of a bitmap matching [width], [height], and [config]. */
    fun stringify(@Px width: Int, @Px height: Int, config: Bitmap.Config): String
}

/** A strategy that requires a [Bitmap]'s size to be greater than or equal to the requested size. */
@VisibleForTesting
@RequiresApi(19)
internal class SizeStrategy : BitmapPoolStrategy {

    private val entries = LinkedMultimap<Int, Bitmap>()
    private val sizes = TreeMap<Int, Int>()

    override fun put(bitmap: Bitmap) {
        val size = bitmap.allocationByteCountCompat
        entries.put(size, bitmap)

        val count = sizes[size]
        sizes[size] = if (count == null) 1 else count + 1
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        val size = Utils.calculateAllocationByteCount(width, height, config)

        // Find the least key greater than or equal to size.
        val bestSize = sizes.ceilingKey(size)?.takeIf { it <= MAX_SIZE_MULTIPLE * size } ?: size

        // Always call removeLast so bestSize becomes the head of the linked list.
        val bitmap = entries.removeLast(bestSize)
        if (bitmap != null) {
            decrementSize(bestSize)
            bitmap.reconfigure(width, height, config)
        }
        return bitmap
    }

    override fun removeLast(): Bitmap? {
        val bitmap = entries.removeLast()
        if (bitmap != null) {
            decrementSize(bitmap.allocationByteCount)
        }
        return bitmap
    }

    private fun decrementSize(size: Int) {
        val count = sizes.getValue(size)
        if (count == 1) {
            sizes -= size
        } else {
            sizes[size] = count - 1
        }
    }

    override fun stringify(bitmap: Bitmap) = "[${bitmap.allocationByteCountCompat}]"

    override fun stringify(@Px width: Int, @Px height: Int, config: Bitmap.Config): String {
        return "[${Utils.calculateAllocationByteCount(width, height, config)}]"
    }

    override fun toString() = "SizeStrategy: entries=$entries, sizes=$sizes"

    companion object {
        private const val MAX_SIZE_MULTIPLE = 4
    }
}

/** A strategy that requires a [Bitmap]'s width, height, and config to exactly match the requested attributes. */
@VisibleForTesting
internal class AttributeStrategy : BitmapPoolStrategy {

    private val entries = LinkedMultimap<Key, Bitmap>()

    override fun put(bitmap: Bitmap) {
        entries.put(Key(bitmap.width, bitmap.height, bitmap.config), bitmap)
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        return entries.removeLast(Key(width, height, config))
    }

    override fun removeLast() = entries.removeLast()

    override fun stringify(bitmap: Bitmap) = stringify(bitmap.width, bitmap.height, bitmap.config)

    override fun stringify(width: Int, height: Int, config: Bitmap.Config) = "[$width x $height], $config"

    override fun toString() = "AttributeStrategy: entries=$entries"

    private data class Key(
        @Px val width: Int,
        @Px val height: Int,
        val config: Bitmap.Config
    )
}
