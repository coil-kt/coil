package coil.bitmappool

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.O
import android.util.Log
import androidx.annotation.Px
import androidx.collection.arraySetOf
import coil.bitmappool.strategy.BitmapPoolStrategy
import coil.util.getAllocationByteCountCompat
import coil.util.log

/**
 * A [BitmapPool] implementation that uses a [BitmapPoolStrategy] to bucket [Bitmap]s
 * and then uses an LRU eviction policy to evict [Bitmap]s from the least
 * recently used bucket in order to keep the pool below a given maximum size limit.
 *
 * Adapted from [Glide](https://github.com/bumptech/glide)'s LruBitmapPool.
 * Glide's license information is available [here](https://github.com/bumptech/glide/blob/master/LICENSE).
 */
internal class RealBitmapPool(
    private val maxSize: Long,
    private val allowedConfigs: Set<Bitmap.Config> = getDefaultAllowedConfigs(),
    private val strategy: BitmapPoolStrategy = BitmapPoolStrategy()
) : BitmapPool {

    companion object {
        private const val TAG = "RealBitmapPool"

        @Suppress("DEPRECATION")
        private fun getDefaultAllowedConfigs(): Set<Bitmap.Config> {
            val configs = arraySetOf(
                Bitmap.Config.ALPHA_8,
                Bitmap.Config.RGB_565,
                Bitmap.Config.ARGB_4444,
                Bitmap.Config.ARGB_8888
            )
            if (SDK_INT >= O) {
                configs += Bitmap.Config.RGBA_F16
            }
            return configs
        }
    }

    private var currentSize: Long = 0
    private var hits: Int = 0
    private var misses: Int = 0
    private var puts: Int = 0
    private var evictions: Int = 0

    init {
        require(maxSize >= 0) { "maxSize must be >= 0." }
    }

    @Synchronized
    override fun put(bitmap: Bitmap) {
        require(!bitmap.isRecycled) { "Cannot pool recycled bitmap!" }

        val size = bitmap.getAllocationByteCountCompat()

        if (!bitmap.isMutable || size > maxSize || bitmap.config !in allowedConfigs) {
            log(TAG, Log.VERBOSE) {
                "Rejected bitmap from pool: bitmap: ${strategy.logBitmap(bitmap)}, " +
                    "is mutable: ${bitmap.isMutable}, " +
                    "is greater than max size: ${size > maxSize}" +
                    "is allowed config: ${bitmap.config in allowedConfigs}"
            }
            bitmap.recycle()
            return
        }

        strategy.put(bitmap)

        puts++
        currentSize += size

        log(TAG, Log.VERBOSE) { "Put bitmap in pool=${strategy.logBitmap(bitmap)}" }
        dump()

        trimToSize(maxSize)
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap {
        val result = getOrNull(width, height, config)
        return result ?: Bitmap.createBitmap(width, height, config)
    }

    override fun getOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        val result = getDirtyOrNull(width, height, config)
        result?.eraseColor(Color.TRANSPARENT)
        return result
    }

    override fun getDirty(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap {
        val result = getDirtyOrNull(width, height, config)
        return result ?: Bitmap.createBitmap(width, height, config)
    }

    @Synchronized
    override fun getDirtyOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        assertNotHardwareConfig(config)

        val result = strategy.get(width, height, config)
        if (result == null) {
            log(TAG, Log.DEBUG) { "Missing bitmap=${strategy.logBitmap(width, height, config)}" }
            misses++
        } else {
            hits++
            currentSize -= result.getAllocationByteCountCompat()
            normalize(result)
        }

        log(TAG, Log.VERBOSE) { "Get bitmap=${strategy.logBitmap(width, height, config)}" }
        dump()

        return result
    }

    override fun clear() = clearMemory()

    fun clearMemory() {
        log(TAG, Log.DEBUG) { "clearMemory" }
        trimToSize(-1)
    }

    @Synchronized
    override fun trimMemory(level: Int) {
        log(TAG, Log.DEBUG) { "trimMemory, level=$level" }
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            trimToSize(currentSize / 2)
        }
    }

    /**
     * Setting these two values provides bitmaps that are essentially
     * equivalent to those returned from [Bitmap.createBitmap].
     */
    private fun normalize(bitmap: Bitmap) {
        bitmap.density = Bitmap.DENSITY_NONE
        bitmap.setHasAlpha(true)
        if (SDK_INT >= KITKAT) {
            bitmap.isPremultiplied = true
        }
    }

    @Synchronized
    private fun trimToSize(size: Long) {
        while (currentSize > size) {
            val removed = strategy.removeLast()
            if (removed == null) {
                log(TAG, Log.WARN) { "Size mismatch, resetting.\n${computeUnchecked()}" }
                currentSize = 0
                return
            }
            currentSize -= removed.getAllocationByteCountCompat()
            evictions++

            log(TAG, Log.DEBUG) { "Evicting bitmap=${strategy.logBitmap(removed)}" }
            dump()

            removed.recycle()
        }
    }

    private fun assertNotHardwareConfig(config: Bitmap.Config) {
        require(SDK_INT < O || config != Bitmap.Config.HARDWARE) { "Cannot create a mutable hardware Bitmap." }
    }

    private fun dump() {
        log(TAG, Log.VERBOSE) { computeUnchecked() }
    }

    private fun computeUnchecked(): String {
        return "Hits=$hits, misses=$misses, puts=$puts, evictions=$evictions, " +
            "currentSize=$currentSize, maxSize=$maxSize, strategy=$strategy"
    }
}
