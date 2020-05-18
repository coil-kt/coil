package coil.bitmappool

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.Px

/**
 * An object pool that enables callers to reuse [Bitmap] objects.
 */
interface BitmapPool {

    companion object {
        /**
         * Create a new [BitmapPool].
         *
         * @param maxSize The maximum size of the pool in bytes.
         */
        @JvmStatic
        // @JvmOverloads https://youtrack.jetbrains.com/issue/KT-35716
        @JvmName("create")
        operator fun invoke(maxSize: Int): BitmapPool = RealBitmapPool(maxSize)
    }

    /**
     * Add the given [Bitmap] to the pool if it is eligible to be re-used and the pool can fit it.
     * Otherwise, this method calls [Bitmap.recycle] on the bitmap and discards it.
     *
     * Callers **must not** continue to use the bitmap after calling this method.
     */
    fun put(bitmap: Bitmap)

    /**
     * Return a [Bitmap] of exactly the given width, height, and configuration, and containing only transparent pixels.
     *
     * If no bitmap with the requested attributes is present in the pool, a new one will be allocated.
     *
     * Because this method erases all pixels in the [Bitmap], this method is slightly slower
     * than [getDirty]. If the [Bitmap] is being obtained to be used in [BitmapFactory]
     * or in any other case where every pixel in the [Bitmap] will always be overwritten or cleared,
     * [getDirty] will be faster. When in doubt, use this method to ensure correctness.
     */
    fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap

    /**
     * Identical to [get] except that null will be returned if the pool does not contain a usable bitmap.
     */
    fun getOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?

    /**
     * Identical to [get] except that any returned [Bitmap] may not have been erased and may contain random data.
     *
     * If no bitmap with the requested attributes is present in the pool, a new one will be allocated.
     *
     * Although this method is slightly more efficient than [BitmapPool.get] it should be used with
     * caution and only when the caller is sure that they are going to erase the [Bitmap] entirely
     * before writing new data to it.
     */
    fun getDirty(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap

    /**
     * Identical to [getDirty] except that null will be returned if the pool does not contain a usable bitmap.
     */
    fun getDirtyOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?

    /**
     * @see ComponentCallbacks2.onTrimMemory
     */
    fun trimMemory(level: Int)

    /**
     * Remove all [Bitmap]s from this pool and free their memory.
     */
    fun clear()
}
