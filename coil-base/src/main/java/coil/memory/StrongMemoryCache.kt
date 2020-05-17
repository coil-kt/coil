package coil.memory

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import android.util.Log
import androidx.collection.LruCache
import coil.fetch.Fetcher
import coil.memory.StrongMemoryCache.Key
import coil.memory.StrongMemoryCache.Value
import coil.request.Parameters
import coil.size.Size
import coil.transform.Transformation
import coil.util.Logger
import coil.util.allocationByteCountCompat
import coil.util.log
import coil.util.mapIndices

/**
 * An in-memory cache for [Bitmap]s.
 *
 * NOTE: This class is not thread safe. In practice, it will only be called from the main thread.
 */
internal interface StrongMemoryCache {

    companion object {
        operator fun invoke(
            weakMemoryCache: WeakMemoryCache,
            referenceCounter: BitmapReferenceCounter,
            maxSize: Int,
            logger: Logger?
        ): StrongMemoryCache {
            return when {
                maxSize > 0 -> RealStrongMemoryCache(weakMemoryCache, referenceCounter, maxSize, logger)
                weakMemoryCache is RealWeakMemoryCache -> ForwardingStrongMemoryCache(weakMemoryCache)
                else -> EmptyStrongMemoryCache
            }
        }
    }

    /** The **current size** of the memory cache in bytes. */
    val size: Int

    /** The **maximum size** of the memory cache in bytes. */
    val maxSize: Int

    /** Get the value associated with [key]. */
    fun get(key: Key): Value?

    /** Set the value associated with [key]. */
    fun set(key: Key, bitmap: Bitmap, isSampled: Boolean)

    /** Return the first [Key] matching the given [predicate], or `null` if no such key was found. */
    fun find(predicate: (Key) -> Boolean): Key?

    /** Remove the value referenced by [key] from this cache if it is present. */
    fun remove(key: Key)

    /** Remove all values from this cache. */
    fun clearMemory()

    /** @see ComponentCallbacks2.onTrimMemory */
    fun trimMemory(level: Int)

    /** Cache key for [StrongMemoryCache] and [WeakMemoryCache]. */
    class Key {

        /** The base component of the cache key. This is typically [Fetcher.key]. */
        val baseKey: String

        /** An ordered list of [Transformation.key]s. */
        val transformationKeys: List<String>

        /** The resolved size for the request. This is null if [transformationKeys] is empty. */
        val size: Size?

        /** @see Parameters.cacheKeys */
        val parameterKeys: Map<String, String>

        constructor(
            baseKey: String,
            parameters: Parameters = Parameters.EMPTY
        ) {
            this.baseKey = baseKey
            this.transformationKeys = emptyList()
            this.size = null
            this.parameterKeys = parameters.cacheKeys()
        }

        constructor(
            baseKey: String,
            transformations: List<Transformation>,
            size: Size,
            parameters: Parameters = Parameters.EMPTY
        ) {
            this.baseKey = baseKey
            if (transformations.isEmpty()) {
                this.transformationKeys = emptyList()
                this.size = null
            } else {
                this.transformationKeys = transformations.mapIndices { it.key() }
                this.size = size
            }
            this.parameterKeys = parameters.cacheKeys()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Key &&
                baseKey == other.baseKey &&
                transformationKeys == other.transformationKeys &&
                size == other.size &&
                parameterKeys == other.parameterKeys
        }

        override fun hashCode(): Int {
            var result = baseKey.hashCode()
            result = 31 * result + transformationKeys.hashCode()
            result = 31 * result + (size?.hashCode() ?: 0)
            result = 31 * result + parameterKeys.hashCode()
            return result
        }
    }

    /** Cache value for [StrongMemoryCache] and [WeakMemoryCache]. */
    interface Value {
        val bitmap: Bitmap
        val isSampled: Boolean
    }
}

/** A [StrongMemoryCache] implementation that caches nothing. */
private object EmptyStrongMemoryCache : StrongMemoryCache {

    override val size get() = 0

    override val maxSize get() = 0

    override fun get(key: Key): Value? = null

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean) {}

    override fun find(predicate: (Key) -> Boolean): Key? = null

    override fun remove(key: Key) {}

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [StrongMemoryCache] implementation that caches nothing and delegates to [weakMemoryCache]. */
private class ForwardingStrongMemoryCache(
    private val weakMemoryCache: WeakMemoryCache
) : StrongMemoryCache {

    override val size get() = 0

    override val maxSize get() = 0

    override fun get(key: Key) = weakMemoryCache.get(key)

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean) {
        weakMemoryCache.set(key, bitmap, isSampled, bitmap.allocationByteCountCompat)
    }

    override fun find(predicate: (Key) -> Boolean): Key? = weakMemoryCache.find(predicate)

    override fun remove(key: Key) {}

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [StrongMemoryCache] implementation backed by an [LruCache]. */
private class RealStrongMemoryCache(
    private val weakMemoryCache: WeakMemoryCache,
    private val referenceCounter: BitmapReferenceCounter,
    maxSize: Int,
    private val logger: Logger?
) : StrongMemoryCache {

    private val cache = object : LruCache<Key, InternalValue>(maxSize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: Key,
            oldValue: InternalValue,
            newValue: InternalValue?
        ) {
            val isPooled = referenceCounter.decrement(oldValue.bitmap)
            if (!isPooled) {
                // Add the bitmap to the WeakMemoryCache if it wasn't just added to the BitmapPool.
                weakMemoryCache.set(key, oldValue.bitmap, oldValue.isSampled, oldValue.size)
            }
        }

        override fun sizeOf(key: Key, value: InternalValue) = value.size
    }

    override val size get() = cache.size()

    override val maxSize get() = cache.maxSize()

    override fun get(key: Key) = cache.get(key)

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean) {
        // If the bitmap is too big for the cache, don't even attempt to store it. Doing so will cause
        // the cache to be cleared. Instead just evict an existing element with the same key if it exists.
        val size = bitmap.allocationByteCountCompat
        if (size > maxSize) {
            val previous = cache.remove(key)
            if (previous == null) {
                // If previous != null, the value was already added to the weak memory cache in LruCache.entryRemoved.
                weakMemoryCache.set(key, bitmap, isSampled, size)
            }
            return
        }

        referenceCounter.increment(bitmap)
        cache.put(key, InternalValue(bitmap, isSampled, size))
    }

    override fun find(predicate: (Key) -> Boolean): Key? = cache.snapshot().keys.find(predicate)

    override fun remove(key: Key) {
        logger?.log(TAG, Log.VERBOSE) { "invalidate, key=$key" }
        cache.remove(key)
    }

    override fun clearMemory() {
        logger?.log(TAG, Log.VERBOSE) { "clearMemory" }
        cache.trimToSize(-1)
    }

    override fun trimMemory(level: Int) {
        logger?.log(TAG, Log.VERBOSE) { "trimMemory, level=$level" }
        if (level >= TRIM_MEMORY_BACKGROUND) {
            clearMemory()
        } else if (level in TRIM_MEMORY_RUNNING_LOW until TRIM_MEMORY_UI_HIDDEN) {
            cache.trimToSize(size / 2)
        }
    }

    private class InternalValue(
        override val bitmap: Bitmap,
        override val isSampled: Boolean,
        val size: Int
    ) : Value

    companion object {
        private const val TAG = "RealStrongMemoryCache"
    }
}
