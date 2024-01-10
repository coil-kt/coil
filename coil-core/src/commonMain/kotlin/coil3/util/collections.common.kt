package coil3.util

/**
 * Create a [MutableMap] that orders its entries by most recently used to least recently used.
 *
 * https://youtrack.jetbrains.com/issue/KT-52183
 */
internal expect fun <K : Any, V : Any> LruMutableMap(
    initialCapacity: Int = 0,
    loadFactor: Float = 0.75F,
): MutableMap<K, V>

internal expect fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V>

internal expect fun <T> List<T>.toImmutableList(): List<T>

/** @see forEach */
@PublishedApi // Used by extension modules.
internal inline fun <T> List<T>.forEachIndices(action: (T) -> Unit) {
    for (i in indices) {
        action(get(i))
    }
}

/** @see forEachIndices */
internal inline fun <T> List<T>.forEachIndexedIndices(action: (Int, T) -> Unit) {
    for (i in indices) {
        action(i, get(i))
    }
}

/** @see mapNotNull */
internal inline fun <T, R : Any> List<T>.mapNotNullIndices(transform: (T) -> R?): List<R> {
    val destination = mutableListOf<R>()
    for (i in indices) {
        val value = transform(get(i))
        if (value != null) {
            destination += value
        }
    }
    return destination
}

/** @see flatMap */
internal inline fun <T, R> List<T>.flatMapIndices(transform: (T) -> List<R>): List<R> {
    val destination = mutableListOf<R>()
    for (i in indices) {
        destination += transform(get(i))
    }
    return destination
}

/** @see fold */
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
