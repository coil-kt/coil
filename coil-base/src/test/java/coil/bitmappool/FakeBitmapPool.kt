package coil.bitmappool

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.Px
import coil.util.Utils
import coil.util.allocationByteCountCompat

class FakeBitmapPool : BitmapPool {

    val bitmaps = mutableListOf<Bitmap>()
    val gets = mutableListOf<Get>()

    override fun put(bitmap: Bitmap) {
        bitmaps += bitmap
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap {
        return getDirty(width, height, config)
    }

    override fun getOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        return getDirtyOrNull(width, height, config)
    }

    override fun getDirty(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap {
        return getDirtyOrNull(width, height, config) ?: Bitmap.createBitmap(width, height, config)
    }

    override fun getDirtyOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        require(SDK_INT < 26 || config != Bitmap.Config.HARDWARE)

        gets += Get(width, height, config)

        val size = Utils.calculateAllocationByteCount(width, height, config)
        val index = bitmaps.indexOfFirst { it.allocationByteCountCompat >= size }
        return if (index != -1) {
            val bitmap = bitmaps[index]
            bitmaps.removeAt(index)
            bitmap.apply { reconfigure(width, height, config) }
        } else {
            null
        }
    }

    override fun trimMemory(level: Int) = clear()

    override fun clear() {
        bitmaps.forEach { it.recycle() }
        bitmaps.clear()
    }

    data class Get(
        val width: Int,
        val height: Int,
        val config: Bitmap.Config?
    )
}
