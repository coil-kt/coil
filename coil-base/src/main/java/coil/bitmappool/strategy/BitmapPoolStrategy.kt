package coil.bitmappool.strategy

import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.M
import androidx.annotation.Px

internal interface BitmapPoolStrategy {

    companion object {
        operator fun invoke(): BitmapPoolStrategy {
            return when {
                SDK_INT >= M -> SizeStrategy()
                SDK_INT >= KITKAT -> SizeConfigStrategy()
                else -> AttributeStrategy()
            }
        }
    }

    /**
     * Store [bitmap] in the LRU cache.
     */
    fun put(bitmap: Bitmap)

    /**
     * Get the "best" [Bitmap] for the given attributes.
     * Return null if there is no Bitmap in the cache that can be re-used given those attributes.
     */
    fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap?

    /**
     * Remove the least recently used Bitmap from the cache and return it.
     */
    fun removeLast(): Bitmap?

    /**
     * Log [bitmap].
     */
    fun logBitmap(bitmap: Bitmap): String

    /**
     * Log the given attributes.
     */
    fun logBitmap(@Px width: Int, @Px height: Int, config: Bitmap.Config): String
}
