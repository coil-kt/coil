@file:JvmName("-Collections")

package coil.util

import java.util.Collections

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
internal inline fun <R, T> List<R>.firstNotNullOfOrNullIndices(transform: (R) -> T?): T? {
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
        if (predicate(get(index))) {
            removeAt(index)
            numDeleted++
        }
    }
}

/**
 * Returns a list containing the **non null** results of applying the given
 * [transform] function to each entry in the original map.
 */
internal inline fun <K, V, R : Any> Map<K, V>.mapNotNullValues(
    transform: (Map.Entry<K, V>) -> R?
): Map<K, R> {
    val destination = mutableMapOf<K, R>()
    for (entry in entries) {
        val value = transform(entry)
        if (value != null) {
            destination[entry.key] = value
        }
    }
    return destination
}

internal fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
    1 -> entries.first().let { (key, value) -> Collections.singletonMap(key, value) }
    else -> Collections.unmodifiableMap(LinkedHashMap(this))
}

internal fun <T> List<T>.toImmutableList(): List<T> = when (size) {
    0 -> emptyList()
    1 -> Collections.singletonList(first())
    else -> Collections.unmodifiableList(ArrayList(this))
}
