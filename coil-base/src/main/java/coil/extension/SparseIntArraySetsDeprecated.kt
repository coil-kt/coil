@file:JvmName("SparseIntArraySets")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.extension

import coil.collection.SparseIntArraySet
import coil.collection.addAll as _addAll
import coil.collection.count as _count
import coil.collection.forEach as _forEach
import coil.collection.isEmpty as _isEmpty
import coil.collection.isNotEmpty as _isNotEmpty
import coil.collection.iterator as _iterator
import coil.collection.minusAssign as _minusAssign
import coil.collection.plus as _plus
import coil.collection.plusAssign as _plusAssign

@Deprecated("Replace `coil.extension.count` with `coil.collection.count`.")
inline fun SparseIntArraySet.count() = _count()

@Deprecated("Replace `coil.extension.plusAssign` with `coil.collection.plusAssign`.")
inline operator fun SparseIntArraySet.plusAssign(element: Int) = _plusAssign(element)

@Deprecated("Replace `coil.extension.minusAssign` with `coil.collection.minusAssign`.")
inline operator fun SparseIntArraySet.minusAssign(element: Int) = _minusAssign(element)

@Deprecated("Replace `coil.extension.isEmpty` with `coil.collection.isEmpty`.")
inline fun SparseIntArraySet.isEmpty() = _isEmpty()

@Deprecated("Replace `coil.extension.isNotEmpty` with `coil.collection.isNotEmpty`.")
inline fun SparseIntArraySet.isNotEmpty() = _isNotEmpty()

@Deprecated("Replace `coil.extension.plus` with `coil.collection.plus`.")
operator fun SparseIntArraySet.plus(other: SparseIntArraySet) = _plus(other)

@Deprecated("Replace `coil.extension.addAll` with `coil.collection.addAll`.")
fun SparseIntArraySet.addAll(other: SparseIntArraySet) = _addAll(other)

@Deprecated("Replace `coil.extension.forEach` with `coil.collection.forEach`.")
inline fun SparseIntArraySet.forEach(action: (element: Int) -> Unit) = _forEach(action)

@Deprecated("Replace `coil.extension.iterator` with `coil.collection.iterator`.")
operator fun SparseIntArraySet.iterator() = _iterator()
