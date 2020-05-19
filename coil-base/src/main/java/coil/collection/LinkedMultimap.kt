package coil.collection

import coil.util.removeLast
import java.util.TreeMap

/**
 * An access-ordered map that stores multiple values for each key.
 *
 * @param sorted If true, [LinkedMultimap] will use a [TreeMap] as its backing map.
 *  [ceilingKey] is only useable if the map is created with `sorted = true`.
 *
 * Adapted from [Glide](https://github.com/bumptech/glide)'s GroupedLinkedMap.
 * Glide's license information is available [here](https://github.com/bumptech/glide/blob/master/LICENSE).
 */
internal class LinkedMultimap<K, V>(sorted: Boolean = false) {

    private val head = LinkedEntry<K, V>(null)
    private val entries: MutableMap<K, LinkedEntry<K, V>> = if (sorted) TreeMap() else HashMap()

    /** Add [value] to [key]'s associate value list. */
    fun add(key: K, value: V) {
        val entry = entries.getOrPut(key) {
            LinkedEntry<K, V>(key).apply(::makeTail)
        }
        entry.add(value)
    }

    /** Remove and return a value associated with [key]. */
    fun removeLast(key: K): V? {
        val entry = entries.getOrPut(key) {
            LinkedEntry(key)
        }
        makeHead(entry)
        return entry.removeLast()
    }

    /** Remove and return a value associated with the least recently accessed key. */
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

    /** Return the least key greater than [key]. If no such key exists, return null. */
    fun ceilingKey(key: K): K? {
        check(entries is TreeMap) { "LinkedMultimap is not sorted." }
        return entries.ceilingKey(key)
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
