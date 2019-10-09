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
import coil.bitmappool.strategy.BitmapPoolStrategy
import coil.util.arraySetOf
import coil.util.getAllocationByteCountCompat
import coil.util.log

/**
 * A [BitmapPool] implementation that uses a [BitmapPoolStrategy] to bucket [Bitmap]s
 * and then uses an LRU eviction policy to evict [Bitmap]s from the least
 * recently used bucket in order to keep the pool below a given maximum size limit.
 */
internal class RealBitmapPool(
    private val maxSize: Long,
    private val allowedConfigs: Set<Bitmap.Config> = getDefaultAllowedConfigs(),
    private val strategy: BitmapPoolStrategy = BitmapPoolStrategy()
) : BitmapPool {

    companion object {
        private const val TAG = "RealBitmapPool"

        private fun getDefaultAllowedConfigs(): Set<Bitmap.Config> = arraySetOf {
            addAll(Bitmap.Config.values())

            // Hardware bitmaps cannot be recycled and cannot be added to the pool.
            if (SDK_INT >= O) remove(Bitmap.Config.HARDWARE)
        }
    }

    private var currentSize: Long = 0
    private var hits: Int = 0
    private var misses: Int = 0
    private var puts: Int = 0
    private var evictions: Int = 0

    @Synchronized
    override fun put(bitmap: Bitmap) {
        require(!bitmap.isRecycled) { "Cannot pool recycled bitmap!" }

        val size = bitmap.getAllocationByteCountCompat()

        if (!bitmap.isMutable || size > maxSize || !allowedConfigs.contains(bitmap.config)) {
            log(TAG, Log.VERBOSE) {
                "Rejected bitmap from pool: bitmap: ${strategy.logBitmap(bitmap)}, " +
                    "is mutable: ${bitmap.isMutable}, " +
                    "is greater than max size: ${size > maxSize}" +
                    "is allowed config: ${allowedConfigs.contains(bitmap.config)}"
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
    fun trimMemory(level: Int) {
        log(TAG, Log.DEBUG) { "trimMemory, level=$level" }
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            trimToSize(currentSize / 2)
        }
    }

    /**
     * Setting these two values provides Bitmaps that are essentially
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
