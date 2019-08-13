package coil.bitmappool

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.annotation.Px

/**
 * An object pool that enables callers to reuse [Bitmap] objects.
 */
interface BitmapPool {

    /**
     * Add the given [Bitmap] to the pool if it is eligible to be re-used and the pool can fit it.
     * Otherwise, this method calls [Bitmap.recycle] on the Bitmap and discards it.
     *
     * Callers must **not** continue to use the Bitmap after calling this method.
     */
    fun put(bitmap: Bitmap)

    /**
     * Return a [Bitmap] of exactly the given width, height, and configuration, and containing only transparent pixels.
     *
     * If no Bitmap with the requested attributes is present in the pool, a new one will be allocated.
     *
     * Because this method erases all pixels in the [Bitmap], this method is slightly slower
     * than [getDirty]. If the [Bitmap] is being obtained to be used in [BitmapFactory]
     * or in any other case where every pixel in the [Bitmap] will always be overwritten or cleared,
     * [getDirty] will be faster. When in doubt, use this method to ensure correctness.
     */
    fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap

    /**
     * Identical to [get] except that one [Matrix] is used, but for cases where needs to get a bitmap from an matrix.
     *
     */
    fun getFromMatrix(@Px width: Int, @Px height: Int, config: Bitmap.Config, matrix: Matrix): Bitmap

    /**
     * Identical to [get] except that null will be returned if the pool does not contain a usable Bitmap.
     */
    fun getOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?

    /**
     * Identical to [get] except that any returned [Bitmap] may **not** have been erased and may contain random data.
     *
     * If no Bitmap with the requested attributes is present in the pool, a new one will be allocated.
     *
     * Although this method is slightly more efficient than [BitmapPool.get] it should be used with
     * caution and only when the caller is sure that they are going to erase the [Bitmap] entirely
     * before writing new data to it.
     */
    fun getDirty(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap

    /**
     * Identical to [getDirty] except that null will be returned if the pool does not contain a usable Bitmap.
     */
    fun getDirtyOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?
}
