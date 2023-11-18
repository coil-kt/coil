package coil

import coil.util.toImmutableMap
import kotlin.jvm.JvmField

class Extras private constructor(
    private val data: Map<Key<*>, Any>,
) {

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: Key<T>): T? {
        return data[key] as T?
    }

    fun newBuilder(): Builder {
        return Builder(this)
    }

    class Key<T>(
        val default: T,
    )

    class Builder {
        private val data: MutableMap<Key<*>, Any>

        constructor() {
            data = mutableMapOf()
        }

        constructor(extras: Extras) {
            data = extras.data.toMutableMap()
        }

        operator fun <T> set(key: Key<T>, value: T?) = apply {
            if (value != null) {
                data[key] = value
            } else {
                data -= key
            }
        }

        fun build(): Extras {
            return Extras(data.toImmutableMap())
        }
    }

    companion object {
        @JvmField val EMPTY = Builder().build()
    }
}

fun <T> Extras.getOrDefault(key: Extras.Key<T>): T {
    return this[key] ?: key.default
}

fun <T> Extras.getOrDefault(key: Extras.Key<T>, other: Extras): T {
    return this[key] ?: other[key] ?: key.default
}

fun <T> Extras.getOrDefault(key: Extras.Key<T>, vararg others: Extras): T {
    var value = get(key)
    var index = 0
    while (value == null && index < others.size) {
        value = others[index++][key]
    }
    return value ?: key.default
}
