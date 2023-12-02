package coil3.util

import java.util.Collections

internal actual fun <K : Any, V : Any> LruMutableMap(
    initialCapacity: Int,
    loadFactor: Float,
): MutableMap<K, V> = LinkedHashMap(initialCapacity, loadFactor, true)

internal actual fun <K, V> Map<K, V>.toImmutableMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
    1 -> entries.first().let { (key, value) -> Collections.singletonMap(key, value) }
    else -> Collections.unmodifiableMap(LinkedHashMap(this))
}

internal actual fun <T> List<T>.toImmutableList(): List<T> = when (size) {
    0 -> emptyList()
    1 -> Collections.singletonList(first())
    else -> Collections.unmodifiableList(ArrayList(this))
}
