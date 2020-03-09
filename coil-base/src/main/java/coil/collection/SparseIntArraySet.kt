@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package coil.collection

import android.util.SparseIntArray

/**
 * A collection of unordered, unique [Int]s.
 *
 * This data structure is intended to be more memory efficient than using a [Set] to store [Int]s, both
 * because it avoids auto-boxing elements and it doesn't allocate a hash table.
 *
 * @see SparseIntArray
 */
class SparseIntArraySet(initialCapacity: Int = 10) {

    private var elements = IntArray(initialCapacity)
    private var size = 0

    /** Adds an element to the set. */
    fun add(element: Int): Boolean {
        val i = elements.binarySearch(element, toIndex = size)
        val absent = i < 0
        if (absent) {
            elements = ArrayUtils.insert(elements, size, i.inv(), element)
            size++
        }
        return absent
    }

    /** Removes the element from the set. Return true if it was present. */
    fun remove(element: Int): Boolean {
        val i = elements.binarySearch(element, toIndex = size)
        val present = i >= 0
        if (present) {
            removeAt(i)
        }
        return present
    }

    /** Return true if the SparseIntArraySet contains this element. */
    operator fun contains(element: Int): Boolean = elements.binarySearch(element, toIndex = size) >= 0

    /** Removes the element at the given index. */
    fun removeAt(index: Int) {
        System.arraycopy(elements, index + 1, elements, index, size - (index + 1))
        size--
    }

    /** Returns the number of elements that this SparseIntArraySet currently stores. */
    fun size(): Int = size

    /**
     * Given an index in the range `[0, size)`, returns
     * the element from the `index`th key-value mapping that this
     * SparseIntArraySet stores.
     *
     * The elements corresponding to indices in ascending order are guaranteed to
     * be in ascending order, e.g., `elementAt(0)` will return the
     * smallest element and `elementAt(size()-1)` will return the largest element.
     */
    fun elementAt(index: Int): Int = elements[index]

    /**
     * Returns the index for which [elementAt] would return the
     * specified element, or a negative number if the specified
     * element is not mapped.
     */
    fun indexOfElement(key: Int): Int = elements.binarySearch(key, toIndex = size)

    /** Removes all elements from this SparseIntArraySet. */
    fun clear() {
        size = 0
    }
}
