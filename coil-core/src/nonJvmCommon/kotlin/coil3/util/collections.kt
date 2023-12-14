package coil3.util

import coil3.annotation.Data

internal actual fun <K : Any, V : Any> LruMutableMap(
    initialCapacity: Int,
    loadFactor: Float,
): MutableMap<K, V> = LruMutableMap(LinkedHashMap(initialCapacity, loadFactor))

private class LruMutableMap<K : Any, V : Any>(
    private val delegate: MutableMap<K, V>,
) : MutableMap<K, V> by delegate {

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = delegate.entries.mapTo(mutableSetOf(), ::MutableEntry)

    override fun get(key: K): V? {
        // Remove then re-add the item to move it to the top of the insertion order.
        val item = delegate.remove(key)
        if (item != null) {
            delegate[key] = item
        }
        return item
    }

    override fun put(key: K, value: V): V? {
        // Remove then re-add the item to move it to the top of the insertion order.
        val item = delegate.remove(key)
        delegate[key] = value
        return item
    }

    override fun putAll(from: Map<out K, V>) {
        for ((key, value) in from) {
            put(key, value)
        }
    }

    private inner class MutableEntry(
        private val delegate: MutableMap.MutableEntry<K, V>,
    ) : MutableMap.MutableEntry<K, V> by delegate {

        override fun setValue(newValue: V): V {
            val oldValue = delegate.setValue(newValue)
            put(key, value)
            return oldValue
        }
    }
}

internal actual fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> = ImmutableMap(toMap())

@Data
private class ImmutableMap<K, V>(
    private val delegate: Map<K, V>,
) : Map<K, V> by delegate {

    override val entries: Set<Map.Entry<K, V>>
        get() = delegate.entries.mapTo(mutableSetOf(), ::ImmutableEntry)

    @Data
    private class ImmutableEntry<K, V>(
        private val delegate: Map.Entry<K, V>,
    ) : Map.Entry<K, V> by delegate
}

internal actual fun <T> List<T>.toImmutableList(): List<T> = ImmutableList(toList())

@Data
private class ImmutableList<T>(
    private val delegate: List<T>,
) : List<T> by delegate {

    override fun iterator(): Iterator<T> {
        return ImmutableIterator(delegate.iterator())
    }

    override fun listIterator(): ListIterator<T> {
        return ImmutableListIterator(delegate.listIterator())
    }

    override fun listIterator(index: Int): ListIterator<T> {
        return ImmutableListIterator(delegate.listIterator(index))
    }

    @Data
    private class ImmutableIterator<T>(
        private val delegate: Iterator<T>,
    ) : Iterator<T> by delegate

    @Data
    private class ImmutableListIterator<T>(
        private val delegate: ListIterator<T>,
    ) : ListIterator<T> by delegate
}
