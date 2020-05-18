package coil.memory

import android.content.ComponentCallbacks2
import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.VisibleForTesting
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import coil.util.firstNotNullIndices
import coil.util.identityHashCode
import coil.util.removeIfIndices
import java.lang.ref.WeakReference

/**
 * An in-memory cache that holds weak references to [Bitmap]s.
 *
 * This is used as a secondary caching layer for [MemoryCache]. [MemoryCache] holds strong references to its bitmaps.
 * Bitmaps are added to this cache when they're removed from [MemoryCache].
 *
 * NOTE: This class is not thread safe. In practice, it will only be called from the main thread.
 */
internal interface WeakMemoryCache {

    /** Get the value associated with [key]. */
    fun get(key: Key): Value?

    /** Set the value associated with [key]. */
    fun set(key: Key, bitmap: Bitmap, isSampled: Boolean, size: Int)

    /** Remove the value referenced by [key] from this cache if it is present. */
    fun invalidate(key: Key)

    /** Remove [bitmap] from this cache if it is present. */
    fun invalidate(bitmap: Bitmap)

    /** Remove all values from this cache. */
    fun clearMemory()

    /** @see ComponentCallbacks2.onTrimMemory */
    fun trimMemory(level: Int)
}

/** A [WeakMemoryCache] implementation that holds no references. */
internal object EmptyWeakMemoryCache : WeakMemoryCache {

    override fun get(key: Key): Value? = null

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean, size: Int) {}

    override fun invalidate(key: Key) {}

    override fun invalidate(bitmap: Bitmap) {}

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [WeakMemoryCache] implementation backed by a [HashMap]. */
internal class RealWeakMemoryCache : WeakMemoryCache {

    companion object {
        private const val CLEAN_UP_INTERVAL = 10
    }

    @VisibleForTesting internal val cache = HashMap<Key, ArrayList<WeakValue>>()
    @VisibleForTesting internal var operationsSinceCleanUp = 0

    override fun get(key: Key): Value? {
        val values = cache[key] ?: return null

        // Find the first bitmap that hasn't been collected.
        val strongValue = values.firstNotNullIndices { value ->
            value.reference.get()?.let { bitmap -> StrongValue(bitmap, value.isSampled) }
        }

        cleanUpIfNecessary()

        return strongValue
    }

    override fun set(key: Key, bitmap: Bitmap, isSampled: Boolean, size: Int) {
        val rawValues = cache[key]
        val values = rawValues ?: arrayListOf()

        // Insert the value into the list sorted descending by size.
        run {
            val value = WeakValue(bitmap.identityHashCode, WeakReference(bitmap), isSampled, size)
            for (index in values.indices) {
                if (size >= values[index].size) {
                    values.add(index, value)
                    return@run
                }
            }
            values += value
        }

        // Only put the list if it's not already in the map.
        if (rawValues == null) {
            cache[key] = values
        }

        cleanUpIfNecessary()
    }

    override fun invalidate(key: Key) {
        val value = get(key)
        if (value != null) {
            invalidate(value.bitmap)
        }
    }

    override fun invalidate(bitmap: Bitmap) {
        val identityHashCode = bitmap.identityHashCode

        // Find the bitmap in the cache and remove it.
        run {
            cache.values.forEach { values ->
                for (index in values.indices) {
                    if (values[index].identityHashCode == identityHashCode) {
                        values.removeAt(index)
                        return@run
                    }
                }
            }
        }

        cleanUpIfNecessary()
    }

    /** Remove all values from this cache. */
    override fun clearMemory() {
        cache.clear()
    }

    /** @see ComponentCallbacks2.onTrimMemory */
    override fun trimMemory(level: Int) {
        if (level >= TRIM_MEMORY_RUNNING_LOW && level != TRIM_MEMORY_UI_HIDDEN) {
            cleanUp()
        }
    }

    private fun cleanUpIfNecessary() {
        if (operationsSinceCleanUp++ >= CLEAN_UP_INTERVAL) {
            cleanUp()
        }
    }

    /** Remove any dereferenced bitmaps from the cache. */
    @VisibleForTesting
    internal fun cleanUp() {
        operationsSinceCleanUp = 0

        // Remove all the values whose references have been collected.
        val iterator = cache.values.iterator()
        while (iterator.hasNext()) {
            val list = iterator.next()

            if (list.count() <= 1) {
                // Typically, the list will only contain 1 item. Handle this case in an optimal way here.
                if (list.firstOrNull()?.reference?.get() == null) {
                    iterator.remove()
                }
            } else {
                // Iterate over the list of values and delete individual entries that have been collected.
                if (SDK_INT >= 24) {
                    list.removeIf { it.reference.get() == null }
                } else {
                    list.removeIfIndices { it.reference.get() == null }
                }

                if (list.isEmpty()) {
                    iterator.remove()
                }
            }
        }
    }

    @VisibleForTesting
    internal class WeakValue(
        val identityHashCode: Int,
        val reference: WeakReference<Bitmap>,
        val isSampled: Boolean,
        val size: Int
    )

    private class StrongValue(
        override val bitmap: Bitmap,
        override val isSampled: Boolean
    ) : Value
}
