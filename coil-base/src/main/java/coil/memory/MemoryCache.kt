package coil.memory

import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.FloatRange
import coil.key.Keyer
import coil.util.calculateMemoryCacheSize
import coil.util.defaultMemoryCacheSizePercent
import kotlinx.parcelize.Parcelize

/**
 * An LRU cache of [Bitmap]s.
 */
interface MemoryCache {

    /** The current size of the cache in bytes. */
    val size: Int

    /** The maximum size of the cache in bytes. */
    val maxSize: Int

    /** The keys present in the cache. */
    val keys: Set<Key>

    /** Get the [Value] associated with [key]. */
    operator fun get(key: Key): Value?

    /** Set the [Value] associated with [key]. */
    operator fun set(key: Key, value: Value)

    /**
     * Remove the [Value] referenced by [key].
     *
     * @return 'true' if [key] was present in the cache. Else, return 'false'.
     */
    fun remove(key: Key): Boolean

    /** Remove all values from the memory cache. */
    fun clear()

    /** @see ComponentCallbacks2.onTrimMemory */
    fun trimMemory(level: Int)

    /**
     * The cache key for a [Bitmap] in the memory cache.
     *
     * @param key The value returned by [Keyer.key] (or a custom value).
     * @param extras Extra values that differentiate the associated
     *  cached value from other values with the same [key]. This map
     *  **must be** treated as immutable and should not be modified.
     */
    @Parcelize
    class Key(
        val key: String,
        val extras: Map<String, String> = emptyMap(),
    ) : Parcelable {

        fun copy(
            key: String = this.key,
            extras: Map<String, String> = this.extras
        ) = Key(key, extras)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Key &&
                key == other.key &&
                extras == other.extras
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + extras.hashCode()
            return result
        }

        override fun toString(): String {
            return "Key(key=$key, extras=$extras)"
        }
    }

    /**
     * The value for a [Bitmap] in the memory cache.
     *
     * @param bitmap The cached [Bitmap].
     * @param extras Metadata for [bitmap]. This map **must be**
     *  treated as immutable and should not be modified.
     */
    class Value(
        val bitmap: Bitmap,
        val extras: Map<String, Any> = emptyMap(),
    ) {

        fun copy(
            bitmap: Bitmap = this.bitmap,
            extras: Map<String, Any> = this.extras
        ) = Value(bitmap, extras)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Value &&
                bitmap == other.bitmap &&
                extras == other.extras
        }

        override fun hashCode(): Int {
            var result = bitmap.hashCode()
            result = 31 * result + extras.hashCode()
            return result
        }

        override fun toString(): String {
            return "Value(bitmap=$bitmap, extras=$extras)"
        }
    }

    class Builder(private val context: Context) {

        private var maxSizePercent = defaultMemoryCacheSizePercent(context)
        private var maxSizeBytes = 0
        private var strongReferencesEnabled = true
        private var weakReferencesEnabled = true

        /**
         * Set the maximum size of the memory cache as a percentage of this application's
         * available memory.
         */
        fun maxSizePercent(@FloatRange(from = 0.0, to = 1.0) percent: Double) = apply {
            require(percent in 0.0..1.0) { "size must be in the range [0.0, 1.0]." }
            this.maxSizeBytes = 0
            this.maxSizePercent = percent
        }

        /**
         * Set the maximum size of the memory cache in bytes.
         */
        fun maxSizeBytes(size: Int) = apply {
            require(size >= 0) { "size must be >= 0." }
            this.maxSizePercent = 0.0
            this.maxSizeBytes = size
        }

        /**
         * Enables/disables strong reference tracking of values added to this memory cache.
         */
        fun strongReferencesEnabled(enable: Boolean) = apply {
            this.strongReferencesEnabled = enable
        }

        /**
         * Enables/disables weak reference tracking of values added to this memory cache.
         * Weak references do not contribute to the current size of the memory cache.
         * This ensures that if a [Bitmap] hasn't been garbage collected yet it will be
         * returned from the memory cache.
         */
        fun weakReferencesEnabled(enable: Boolean) = apply {
            this.weakReferencesEnabled = enable
        }

        /**
         * Create a new [MemoryCache] instance.
         */
        fun build(): MemoryCache {
            val weakMemoryCache = if (weakReferencesEnabled) {
                RealWeakMemoryCache()
            } else {
                EmptyWeakMemoryCache()
            }
            val strongMemoryCache = if (strongReferencesEnabled) {
                val maxSize = if (maxSizePercent > 0) {
                    calculateMemoryCacheSize(context, maxSizePercent)
                } else {
                    maxSizeBytes
                }
                if (maxSize > 0) {
                    RealStrongMemoryCache(maxSize, weakMemoryCache)
                } else {
                    EmptyStrongMemoryCache(weakMemoryCache)
                }
            } else {
                EmptyStrongMemoryCache(weakMemoryCache)
            }
            return RealMemoryCache(strongMemoryCache, weakMemoryCache)
        }
    }
}
