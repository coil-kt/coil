@file:JvmName("-Collections")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import androidx.annotation.CheckResult

internal typealias MultiMutableList<R, T> = MutableList<Pair<R, T>>

internal typealias MultiList<R, T> = List<Pair<R, T>>

/**
 * Functionally the same as [Iterable.forEach] except it generates
 * an index-based loop that doesn't use an [Iterator].
 */
internal inline fun <T> List<T>.forEachIndices(action: (T) -> Unit) {
    for (i in indices) {
        action(get(i))
    }
}

/**
 * Functionally the same as [Iterable.map] except it generates
 * an index-based loop that doesn't use an [Iterator].
 */
internal inline fun <R, T> List<R>.mapIndices(transform: (R) -> T): List<T> {
    val destination = ArrayList<T>(size)
    for (i in indices) {
        destination += transform(get(i))
    }
    return destination
}

/**
 * Functionally the same as [Iterable.find] except it generates
 * an index-based loop that doesn't use an [Iterator].
 */
internal inline fun <T> List<T>.findIndices(predicate: (T) -> Boolean): T? {
    for (i in indices) {
        val value = get(i)
        if (predicate(value)) {
            return value
        }
    }
    return null
}

/**
 * Functionally the same as [Iterable.fold] except it generates
 * an index-based loop that doesn't use an [Iterator].
 */
internal inline fun <T, R> List<T>.foldIndices(initial: R, operation: (R, T) -> R): R {
    var accumulator = initial
    for (i in indices) {
        accumulator = operation(accumulator, get(i))
    }
    return accumulator
}

/**
 * Return the first non-null value returned by [transform].
 * Generate an index-based loop that doesn't use an [Iterator].
 */
internal inline fun <R, T> List<R>.firstNotNullIndices(transform: (R) -> T?): T? {
    for (i in indices) {
        transform(get(i))?.let { return it }
    }
    return null
}

/**
 * Removes values from the list as determined by the [predicate].
 * Generate an index-based loop that doesn't use an [Iterator].
 */
internal inline fun <T> MutableList<T>.removeIfIndices(predicate: (T) -> Boolean) {
    var numDeleted = 0

    for (rawIndex in indices) {
        val index = rawIndex - numDeleted
        val value = get(index)

        if (predicate(value)) {
            removeAt(index)
            numDeleted++
        }
    }
}

internal inline fun <T> MutableList<T>.removeLast(): T? {
    return if (isNotEmpty()) removeAt(lastIndex) else null
}

/**
 * Returns a list containing the **non null** results of applying the given
 * [transform] function to each entry in the original map.
 */
internal inline fun <K, V, R : Any> Map<K, V>.mapNotNullValues(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
    val destination = mutableMapOf<K, R>()
    for (entry in this) {
        val value = transform(entry)
        if (value != null) {
            destination[entry.key] = value
        }
    }
    return destination
}

/**
 * Inserts an element into the array at the specified index, growing the array if there is no more room.
 *
 * Adapted from Android's GrowingArrayUtils.
 *
 * @param index The index to insert at.
 * @param element The element to insert.
 * @param currentSize The number of elements in the array. Must be less than or equal to array.size.
 * @return The array to which the element was appended. This may be different than the given array.
 */
@CheckResult
internal fun IntArray.growAndInsert(index: Int, element: Int, currentSize: Int): IntArray {
    // Fast path: insert into the given array.
    if (currentSize + 1 <= size) {
        copyInto(this, destinationOffset = index + 1, startIndex = index, endIndex = currentSize)
        this[index] = element
        return this
    }

    // Slow path: create a new, larger array and copy over the elements.
    val newSize = if (currentSize <= 4) 8 else currentSize * 2
    val newArray = IntArray(newSize)
    copyInto(newArray, endIndex = index)
    newArray[index] = element
    copyInto(newArray, destinationOffset = index + 1, startIndex = index, endIndex = size)
    return newArray
}
