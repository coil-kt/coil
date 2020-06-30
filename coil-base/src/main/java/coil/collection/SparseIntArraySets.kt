@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.collection

/** Returns the number of elements that this set currently stores. */
inline fun SparseIntArraySet.count(): Int = size

/** Adds the element to the set. */
inline operator fun SparseIntArraySet.plusAssign(element: Int) {
    add(element)
}

/** Removes the element from the set. */
inline operator fun SparseIntArraySet.minusAssign(element: Int) {
    remove(element)
}

/** Return true when the set contains no elements. */
inline fun SparseIntArraySet.isEmpty(): Boolean = size == 0

/** Return true when the set contains elements. */
inline fun SparseIntArraySet.isNotEmpty(): Boolean = size != 0

/** Create and return a new set that contains the elements of [this] plus the elements of [other]. */
operator fun SparseIntArraySet.plus(other: SparseIntArraySet): SparseIntArraySet {
    val new = SparseIntArraySet(size + other.size)
    new.addAll(this)
    new.addAll(other)
    return new
}

/** Add all elements from [other] to [this]. */
fun SparseIntArraySet.addAll(other: SparseIntArraySet) {
    other.forEach { add(it) }
}

/** Performs the given [action] for each element in the set. */
inline fun SparseIntArraySet.forEach(action: (element: Int) -> Unit) {
    for (index in 0 until size) {
        action(elementAt(index))
    }
}

/** Return an iterator over the set's values. */
operator fun SparseIntArraySet.iterator(): IntIterator = object : IntIterator() {
    var index = 0
    override fun hasNext() = index < size
    override fun nextInt() = elementAt(index++)
}
