package coil.memory

import android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW
import android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
import android.graphics.Bitmap
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.VisibleForTesting
import coil.memory.MemoryCache.Key
import coil.memory.MemoryCache.Value
import coil.util.firstNotNullOfOrNullIndices
import coil.util.identityHashCode
import coil.util.removeIfIndices
import java.lang.ref.WeakReference

/**
 * An in-memory cache that holds weak references to [Bitmap]s.
 *
 * Bitmaps are added to [WeakMemoryCache] when they're removed from [StrongMemoryCache].
 */
internal interface WeakMemoryCache {

    val keys: Set<Key>

    fun get(key: Key): Value?

    fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>, size: Int)

    fun remove(key: Key): Boolean

    fun clearMemory()

    fun trimMemory(level: Int)
}

/** A [WeakMemoryCache] implementation that holds no references. */
internal class EmptyWeakMemoryCache : WeakMemoryCache {

    override val keys get() = emptySet<Key>()

    override fun get(key: Key): Value? = null

    override fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>, size: Int) {}

    override fun remove(key: Key) = false

    override fun clearMemory() {}

    override fun trimMemory(level: Int) {}
}

/** A [WeakMemoryCache] implementation backed by a [HashMap]. */
internal class RealWeakMemoryCache : WeakMemoryCache {

    @VisibleForTesting internal val cache = LinkedHashMap<Key, ArrayList<InternalValue>>()
    private var operationsSinceCleanUp = 0

    override val keys @Synchronized get() = cache.keys.toSet()

    @Synchronized
    override fun get(key: Key): Value? {
        val values = cache[key] ?: return null

        // Find the first bitmap that hasn't been collected.
        val value = values.firstNotNullOfOrNullIndices { value ->
            value.bitmap.get()?.let { Value(it, value.extras) }
        }

        cleanUpIfNecessary()
        return value
    }

    @Synchronized
    override fun set(key: Key, bitmap: Bitmap, extras: Map<String, Any>, size: Int) {
        val values = cache.getOrPut(key) { arrayListOf() }

        // Insert the value into the list sorted descending by size.
        run {
            val identityHashCode = bitmap.identityHashCode
            val newValue = InternalValue(identityHashCode, WeakReference(bitmap), extras, size)
            for (index in values.indices) {
                val value = values[index]
                if (size >= value.size) {
                    if (value.identityHashCode == identityHashCode && value.bitmap.get() === bitmap) {
                        values[index] = newValue
                    } else {
                        values.add(index, newValue)
                    }
                    return@run
                }
            }
            values += newValue
        }

        cleanUpIfNecessary()
    }

    @Synchronized
    override fun remove(key: Key): Boolean {
        return cache.remove(key) != null
    }

    @Synchronized
    override fun clearMemory() {
        operationsSinceCleanUp = 0
        cache.clear()
    }

    @Synchronized
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
                if (list.firstOrNull()?.bitmap?.get() == null) {
                    iterator.remove()
                }
            } else {
                // Iterate over the list of values and delete individual entries that have been collected.
                if (SDK_INT >= 24) {
                    list.removeIf { it.bitmap.get() == null }
                } else {
                    list.removeIfIndices { it.bitmap.get() == null }
                }

                if (list.isEmpty()) {
                    iterator.remove()
                }
            }
        }
    }

    @VisibleForTesting
    internal class InternalValue(
        val identityHashCode: Int,
        val bitmap: WeakReference<Bitmap>,
        val extras: Map<String, Any>,
        val size: Int
    )

    companion object {
        private const val CLEAN_UP_INTERVAL = 10
    }
}
