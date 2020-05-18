package coil.bitmappool

import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.Px
import androidx.core.graphics.createBitmap
import coil.util.Logger
import coil.util.allocationByteCountCompat
import coil.util.isHardware
import coil.util.log

/**
 * A [BitmapPool] implementation that uses a [ReuseStrategy] to bucket [Bitmap]s
 * and then uses an LRU eviction policy to evict [Bitmap]s from the least
 * recently used bucket in order to keep the pool below a given maximum size limit.
 */
internal class RealBitmapPool(
    private val maxSize: Int,
    private val allowedConfigs: Set<Bitmap.Config> = ALLOWED_CONFIGS,
    private val strategy: ReuseStrategy = ReuseStrategy(),
    private val logger: Logger? = null
) : BitmapPool {

    companion object {
        private const val TAG = "RealBitmapPool"

        @Suppress("DEPRECATION")
        @OptIn(ExperimentalStdlibApi::class)
        private val ALLOWED_CONFIGS = buildSet {
            add(Bitmap.Config.ALPHA_8)
            add(Bitmap.Config.RGB_565)
            add(Bitmap.Config.ARGB_4444)
            add(Bitmap.Config.ARGB_8888)
            if (SDK_INT >= 26) add(Bitmap.Config.RGBA_F16)
        }
    }

    private val bitmaps = hashSetOf<Bitmap>()

    private var currentSize = 0
    private var hits = 0
    private var misses = 0
    private var puts = 0
    private var evictions = 0

    init {
        require(maxSize >= 0) { "maxSize must be >= 0." }
    }

    @Synchronized
    override fun put(bitmap: Bitmap) {
        require(!bitmap.isRecycled) { "Cannot pool a recycled bitmap." }

        if (bitmap in bitmaps) {
            logger?.log(TAG, Log.VERBOSE) {
                "Rejecting duplicate bitmap from pool: bitmap: ${strategy.stringify(bitmap)}"
            }
            return
        }

        val size = bitmap.allocationByteCountCompat

        if (!bitmap.isMutable || size > maxSize || bitmap.config !in allowedConfigs) {
            logger?.log(TAG, Log.VERBOSE) {
                "Rejecting bitmap from pool: bitmap: ${strategy.stringify(bitmap)}, " +
                    "is mutable: ${bitmap.isMutable}, " +
                    "is greater than max size: ${size > maxSize}" +
                    "is allowed config: ${bitmap.config in allowedConfigs}"
            }
            bitmap.recycle()
            return
        }

        bitmaps += bitmap
        strategy.put(bitmap)

        puts++
        currentSize += size

        logger?.log(TAG, Log.VERBOSE) { "Put bitmap=${strategy.stringify(bitmap)}\n${logStats()}" }

        trimToSize(maxSize)
    }

    override fun get(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap {
        return getOrNull(width, height, config) ?: createBitmap(width, height, config)
    }

    override fun getOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        return getDirtyOrNull(width, height, config)?.apply { eraseColor(Color.TRANSPARENT) }
    }

    override fun getDirty(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap {
        return getDirtyOrNull(width, height, config) ?: createBitmap(width, height, config)
    }

    @Synchronized
    override fun getDirtyOrNull(@Px width: Int, @Px height: Int, config: Bitmap.Config): Bitmap? {
        require(!config.isHardware) { "Cannot create a mutable hardware bitmap." }

        val result = strategy.get(width, height, config)
        if (result == null) {
            logger?.log(TAG, Log.VERBOSE) { "Missing bitmap=${strategy.stringify(width, height, config)}" }
            misses++
        } else {
            hits++
            currentSize -= result.allocationByteCountCompat
            bitmaps -= result
            normalize(result)
        }

        logger?.log(TAG, Log.VERBOSE) { "Get bitmap=${strategy.stringify(width, height, config)}\n${logStats()}" }

        return result
    }

    override fun clear() = clearMemory()

    fun clearMemory() {
        logger?.log(TAG, Log.VERBOSE) { "clearMemory" }
        trimToSize(-1)
    }

    @Synchronized
    override fun trimMemory(level: Int) {
        logger?.log(TAG, Log.VERBOSE) { "trimMemory, level=$level" }
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
        if (SDK_INT >= 19) {
            bitmap.isPremultiplied = true
        }
    }

    @Synchronized
    private fun trimToSize(size: Int) {
        while (currentSize > size) {
            val removed = strategy.removeLast()
            if (removed == null) {
                logger?.log(TAG, Log.WARN) { "Size mismatch, resetting.\n${logStats()}" }
                currentSize = 0
                return
            }

            currentSize -= removed.allocationByteCountCompat
            bitmaps -= removed
            evictions++

            logger?.log(TAG, Log.VERBOSE) { "Evicting bitmap=${strategy.stringify(removed)}\n${logStats()}" }

            removed.recycle()
        }
    }

    private fun logStats(): String {
        return "Hits=$hits, misses=$misses, puts=$puts, evictions=$evictions, " +
            "currentSize=$currentSize, maxSize=$maxSize, strategy=$strategy"
    }
}
