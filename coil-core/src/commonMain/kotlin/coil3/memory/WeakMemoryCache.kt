package coil3.memory

import coil3.Image
import coil3.annotation.VisibleForTesting
import coil3.memory.MemoryCache.Key
import coil3.memory.MemoryCache.Value
import coil3.util.WeakReference
import coil3.util.firstNotNullOfOrNullIndices
import coil3.util.removeIfIndices
import kotlin.experimental.ExperimentalNativeApi

/**
 * An in-memory cache that holds weak references.
 *
 * Values are added to [WeakMemoryCache] when they're evicted from [StrongMemoryCache].
 */
internal interface WeakMemoryCache {

    val keys: Set<Key>

    fun get(key: Key): Value?

    fun set(key: Key, image: Image, extras: Map<String, Any>, size: Long)

    fun remove(key: Key): Boolean

    fun clear()
}

internal class EmptyWeakMemoryCache : WeakMemoryCache {

    override val keys get() = emptySet<Key>()

    override fun get(key: Key): Value? = null

    override fun set(key: Key, image: Image, extras: Map<String, Any>, size: Long) {}

    override fun remove(key: Key) = false

    override fun clear() {}
}

/** A [WeakMemoryCache] implementation backed by a [LinkedHashMap]. */
@OptIn(ExperimentalNativeApi::class)
internal class RealWeakMemoryCache : WeakMemoryCache {

    internal val cache = LinkedHashMap<Key, ArrayList<InternalValue>>()
    private var operationsSinceCleanUp = 0

    override val keys: Set<Key>
        get() = cache.keys.toSet()

    override fun get(key: Key): Value? {
        val values = cache[key] ?: return null

        // Find the first image that hasn't been collected.
        val value = values.firstNotNullOfOrNullIndices { value ->
            value.image.get()?.let { Value(it, value.extras) }
        }

        cleanUpIfNecessary()
        return value
    }

    override fun set(
        key: Key,
        image: Image,
        extras: Map<String, Any>,
        size: Long,
    ) {
        val values = cache.getOrPut(key) { arrayListOf() }

        // Insert the value into the list sorted descending by size.
        val newValue = InternalValue(WeakReference(image), extras, size)
        if (values.isEmpty()) {
            values += newValue
        } else {
            for (index in values.indices) {
                val value = values[index]
                if (size >= value.size) {
                    if (value.image.get() === image) {
                        values[index] = newValue
                    } else {
                        values.add(index, newValue)
                    }
                    break
                }
            }
        }

        cleanUpIfNecessary()
    }

    override fun remove(key: Key): Boolean {
        return cache.remove(key) != null
    }

    override fun clear() {
        operationsSinceCleanUp = 0
        cache.clear()
    }

    private fun cleanUpIfNecessary() {
        if (operationsSinceCleanUp++ >= CLEAN_UP_INTERVAL) {
            cleanUp()
        }
    }

    /** Remove any dereferenced images from the cache. */
    @VisibleForTesting
    internal fun cleanUp() {
        operationsSinceCleanUp = 0

        // Remove all the values whose references have been collected.
        val iterator = cache.values.iterator()
        while (iterator.hasNext()) {
            val list = iterator.next()

            if (list.size <= 1) {
                // Typically, the list will only contain 1 item. Handle this case in an optimal way here.
                if (list.firstOrNull()?.image?.get() == null) {
                    iterator.remove()
                }
            } else {
                // Iterate over the list of values and delete individual entries that have been collected.
                list.removeIfIndices { it.image.get() == null }

                if (list.isEmpty()) {
                    iterator.remove()
                }
            }
        }
    }

    @VisibleForTesting
    internal class InternalValue(
        val image: WeakReference<Image>,
        val extras: Map<String, Any>,
        val size: Long,
    )

    companion object {
        private const val CLEAN_UP_INTERVAL = 10
    }
}
