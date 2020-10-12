package coil.bitmap

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import coil.util.isHardware

/**
 * A lock-free [BitmapPool] implementation that stores nothing and
 * immediately recycles any [Bitmap]s that are added to the pool.
 */
internal class EmptyBitmapPool : BitmapPool {

    override fun put(bitmap: Bitmap) {
        bitmap.recycle()
    }

    override fun get(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        require(!config.isHardware) { "Cannot create a mutable hardware bitmap." }
        return createBitmap(width, height, config)
    }

    override fun getOrNull(width: Int, height: Int, config: Bitmap.Config): Bitmap? = null

    override fun getDirty(width: Int, height: Int, config: Bitmap.Config): Bitmap = get(width, height, config)

    override fun getDirtyOrNull(width: Int, height: Int, config: Bitmap.Config): Bitmap? = null

    override fun trimMemory(level: Int) {}

    override fun clear() {}
}
