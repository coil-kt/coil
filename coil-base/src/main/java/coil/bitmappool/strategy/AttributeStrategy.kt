package coil.bitmappool.strategy

import android.graphics.Bitmap
import androidx.annotation.Px
import coil.collection.GroupedLinkedMap

/**
 * A strategy for reusing bitmaps that requires any returned bitmap's dimensions to exactly match those in the request.
 */
internal class AttributeStrategy : BitmapPoolStrategy {

    companion object {
        @Suppress("NOTHING_TO_INLINE")
        private inline fun getBitmapString(width: Int, height: Int, config: Bitmap.Config) = "[$width x $height], $config"
    }

    private val groupedMap = GroupedLinkedMap<Key, Bitmap>()

    override fun put(bitmap: Bitmap) {
        groupedMap[Key(bitmap.width, bitmap.height, bitmap.config)] = bitmap
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        return groupedMap[Key(width, height, config)]
    }

    override fun removeLast(): Bitmap? {
        return groupedMap.removeLast()
    }

    override fun logBitmap(bitmap: Bitmap): String {
        return getBitmapString(bitmap.width, bitmap.height, bitmap.config)
    }

    override fun logBitmap(@Px width: Int, @Px height: Int, config: Bitmap.Config): String {
        return getBitmapString(width, height, config)
    }

    override fun toString() = "AttributeStrategy: groupedMap=$groupedMap"

    private data class Key(
        @Px val width: Int,
        @Px val height: Int,
        val config: Bitmap.Config
    ) {
        override fun toString() = getBitmapString(width, height, config)
    }
}
