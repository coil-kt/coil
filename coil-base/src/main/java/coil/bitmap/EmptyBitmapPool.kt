package coil.bitmap

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import coil.util.isHardware

/** A lock-free [BitmapPool] implementation that recycles any [Bitmap]s that are added to it. */
internal class EmptyBitmapPool : BitmapPool {

    override fun put(bitmap: Bitmap) {
        bitmap.recycle()
    }

    override fun get(width: Int, height: Int, config: Bitmap.Config) = getDirty(width, height, config)

    override fun getOrNull(width: Int, height: Int, config: Bitmap.Config) = getDirtyOrNull(width, height, config)

    override fun getDirty(width: Int, height: Int, config: Bitmap.Config): Bitmap {
        assertNotHardware(config)
        return createBitmap(width, height, config)
    }

    override fun getDirtyOrNull(width: Int, height: Int, config: Bitmap.Config): Bitmap? {
        assertNotHardware(config)
        return null
    }

    override fun trimMemory(level: Int) {}

    override fun clear() {}

    private fun assertNotHardware(config: Bitmap.Config) {
        require(!config.isHardware) { "Cannot create a mutable hardware bitmap." }
    }
}
