@file:JvmName("SparseIntArraySets")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.extension

import coil.collection.SparseIntArraySet

inline operator fun SparseIntArraySet.plusAssign(element: Int) {
    add(element)
}

inline operator fun SparseIntArraySet.minusAssign(element: Int) {
    remove(element)
}

inline fun SparseIntArraySet.isEmpty(): Boolean = size() == 0

inline fun SparseIntArraySet.isNotEmpty(): Boolean = size() != 0

operator fun SparseIntArraySet.plus(other: SparseIntArraySet): SparseIntArraySet {
    val new = SparseIntArraySet(size() + other.size())
    new.addAll(this)
    new.addAll(other)
    return new
}

fun SparseIntArraySet.addAll(other: SparseIntArraySet) {
    other.forEach { add(it) }
}

inline fun SparseIntArraySet.forEach(action: (element: Int) -> Unit) {
    for (index in 0 until size()) {
        action(elementAt(index))
    }
}
