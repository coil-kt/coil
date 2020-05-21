package coil.collection

/**
 * An access-ordered map that stores multiple values for each key.
 *
 * Adapted from [Glide](https://github.com/bumptech/glide)'s GroupedLinkedMap.
 * Glide's license information is available [here](https://github.com/bumptech/glide/blob/master/LICENSE).
 */
internal class LinkedMultimap<K, V> {

    private val head = LinkedEntry<K, V>(null)
    private val entries = HashMap<K, LinkedEntry<K, V>>()

    /** Add [value] to [key]'s associate value list. */
    fun put(key: K, value: V) {
        val entry = entries.getOrPut(key) {
            LinkedEntry<K, V>(key).apply(::makeTail)
        }
        entry.add(value)
    }

    /** Remove and return the last value associated with [key]. */
    fun removeLast(key: K): V? {
        val entry = entries.getOrPut(key) {
            LinkedEntry(key)
        }
        makeHead(entry)
        return entry.removeLast()
    }

    /** Remove and return the last value associated with the least recently accessed key. */
    fun removeLast(): V? {
        var last = head.prev

        while (last != head) {
            val removed = last.removeLast()
            if (removed != null) {
                return removed
            } else {
                removeEntry(last)
                entries.remove(last.key)
            }
            last = last.prev
        }

        return null
    }

    override fun toString() = buildString {
        append("LinkedMultimap( ")

        var current = head.next

        while (current != head) {
            append('{')
            append(current.key)
            append(':')
            append(current.size)
            append('}')

            current = current.next
            if (current != head) append(", ")
        }

        append(" )")
    }

    /** Make [entry] the most recently used item. */
    private fun makeHead(entry: LinkedEntry<K, V>) {
        removeEntry(entry)
        entry.prev = head
        entry.next = head.next
        insertEntry(entry)
    }

    /** Make [entry] the least recently used item. */
    private fun makeTail(entry: LinkedEntry<K, V>) {
        removeEntry(entry)
        entry.prev = head.prev
        entry.next = head
        insertEntry(entry)
    }

    /** Update [entry]'s neighbors to reference [entry]. */
    private fun <K, V> insertEntry(entry: LinkedEntry<K, V>) {
        entry.next.prev = entry
        entry.prev.next = entry
    }

    /** Update [entry]'s neighbors to reference each other. */
    private fun <K, V> removeEntry(entry: LinkedEntry<K, V>) {
        entry.prev.next = entry.next
        entry.next.prev = entry.prev
    }

    @OptIn(ExperimentalStdlibApi::class)
    private class LinkedEntry<K, V>(val key: K?) {

        private var values: MutableList<V>? = null

        var prev: LinkedEntry<K, V> = this
        var next: LinkedEntry<K, V> = this

        val size: Int get() = values?.size ?: 0

        fun removeLast(): V? = values?.removeLastOrNull()

        fun add(value: V) {
            (values ?: mutableListOf<V>().also { values = it }) += value
        }
    }
}
