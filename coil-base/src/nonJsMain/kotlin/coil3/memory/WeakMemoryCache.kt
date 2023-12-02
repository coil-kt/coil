package coil3.memory

import coil3.Image
import coil3.annotation.VisibleForTesting
import coil3.memory.MemoryCache.Key
import coil3.util.WeakReference
import coil3.util.firstNotNullOfOrNullIndices
import coil3.util.identityHashCode
import coil3.util.removeIfIndices
import kotlin.experimental.ExperimentalNativeApi
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal actual fun RealWeakMemoryCache(): WeakMemoryCache = WeakReferenceMemoryCache()

/** A [WeakMemoryCache] implementation backed by a [LinkedHashMap]. */
@OptIn(ExperimentalNativeApi::class)
internal class WeakReferenceMemoryCache : WeakMemoryCache {

    private val lock = SynchronizedObject()
    internal val cache = LinkedHashMap<Key, ArrayList<InternalValue>>()
    private var operationsSinceCleanUp = 0

    override val keys: Set<Key>
        get() = synchronized(lock) { cache.keys.toSet() }

    override fun get(key: Key): MemoryCache.Value? = synchronized(lock) {
        val values = cache[key] ?: return null

        // Find the first image that hasn't been collected.
        val value = values.firstNotNullOfOrNullIndices { value ->
            value.image.get()?.let { MemoryCache.Value(it, value.extras) }
        }

        cleanUpIfNecessary()
        return value
    }

    override fun set(
        key: Key,
        image: Image,
        extras: Map<String, Any>,
        size: Long,
    ) = synchronized(lock) {
        val values = cache.getOrPut(key) { arrayListOf() }

        // Insert the value into the list sorted descending by size.
        run {
            val identityHashCode = image.identityHashCode()
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

    override fun remove(key: Key): Boolean = synchronized(lock) {
        return cache.remove(key) != null
    }

    override fun clear() = synchronized(lock) {
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
        val identityHashCode: Int,
        val image: WeakReference<Image>,
        val extras: Map<String, Any>,
        val size: Long,
    )

    companion object {
        private const val CLEAN_UP_INTERVAL = 10
    }
}
