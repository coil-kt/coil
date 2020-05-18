package coil.collection

import coil.util.removeLast

/**
 * An access-ordered map that stores multiple values for each key.
 *
 * Adapted from [Glide](https://github.com/bumptech/glide)'s GroupedLinkedMap.
 * Glide's license information is available [here](https://github.com/bumptech/glide/blob/master/LICENSE).
 */
internal class LinkedMultimap<K, V> {

    private val head = LinkedEntry<K, V>(null)
    private val map = HashMap<K, LinkedEntry<K, V>>()

    operator fun set(key: K, value: V) {
        val entry = map.getOrPut(key) {
            LinkedEntry<K, V>(key).apply(::makeTail)
        }
        entry.add(value)
    }

    operator fun get(key: K): V? {
        val entry = map.getOrPut(key) {
            LinkedEntry(key)
        }
        makeHead(entry)
        return entry.removeLast()
    }

    fun removeLast(): V? {
        var last = head.prev

        while (last != head) {
            val removed = last.removeLast()
            if (removed != null) {
                return removed
            } else {
                // Remove the empty LinkedEntry.
                removeEntry(last)
                map.remove(last.key)
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

    private class LinkedEntry<K, V>(@JvmField val key: K?) {

        private var values: MutableList<V>? = null

        @JvmField var prev: LinkedEntry<K, V> = this
        @JvmField var next: LinkedEntry<K, V> = this

        val size: Int get() = values?.size ?: 0

        fun removeLast(): V? = values?.removeLast()

        fun add(value: V) {
            (values ?: mutableListOf<V>().also { values = it }) += value
        }
    }
}
