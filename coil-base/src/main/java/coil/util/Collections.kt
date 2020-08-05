@file:JvmName("-Collections")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

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

/**
 * Returns a list containing the **non null** results of applying the given
 * [transform] function to each entry in the original map.
 */
internal inline fun <K, V, R : Any> Map<K, V>.mapNotNullValues(transform: (Map.Entry<K, V>) -> R?): Map<K, R> {
    val destination = mutableMapOf<K, R>()
    for (entry in entries) {
        val value = transform(entry)
        if (value != null) {
            destination[entry.key] = value
        }
    }
    return destination
}
