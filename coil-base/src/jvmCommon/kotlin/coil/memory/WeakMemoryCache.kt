package coil.memory

import coil.Image
import coil.annotation.VisibleForTesting
import coil.memory.MemoryCache.Key
import coil.util.firstNotNullOfOrNullIndices
import coil.util.identityHashCode
import coil.util.removeIfIndices
import java.lang.ref.WeakReference

internal actual fun RealWeakMemoryCache(): WeakMemoryCache = WeakReferenceMemoryCache()

/** A [WeakMemoryCache] implementation backed by a [LinkedHashMap]. */
internal class WeakReferenceMemoryCache : WeakMemoryCache {

    @VisibleForTesting
    internal val cache = LinkedHashMap<Key, ArrayList<InternalValue>>()
    private var operationsSinceCleanUp = 0

    override val keys @Synchronized get() = cache.keys.toSet()

    @Synchronized
    override fun get(key: Key): MemoryCache.Value? {
        val values = cache[key] ?: return null

        // Find the first bitmap that hasn't been collected.
        val value = values.firstNotNullOfOrNullIndices { value ->
            value.image.get()?.let { MemoryCache.Value(it, value.extras) }
        }

        cleanUpIfNecessary()
        return value
    }

    @Synchronized
    override fun set(key: Key, image: Image, extras: Map<String, Any>, size: Long) {
        val values = cache.getOrPut(key) { arrayListOf() }

        // Insert the value into the list sorted descending by size.
        run {
            val identityHashCode = image.identityHashCode
            val newValue = InternalValue(identityHashCode, WeakReference(image), extras, size)
            for (index in values.indices) {
                val value = values[index]
                if (size >= value.size) {
                    if (value.identityHashCode == identityHashCode && value.image.get() === image) {
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
    override fun clear() {
        operationsSinceCleanUp = 0
        cache.clear()
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
        val identityHashCode: Int,
        val image: WeakReference<Image>,
        val extras: Map<String, Any>,
        val size: Long,
    )

    companion object {
        private const val CLEAN_UP_INTERVAL = 10
    }
}
