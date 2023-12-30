package coil3

import coil3.annotation.Data
import coil3.request.ImageRequest
import coil3.request.Options
import coil3.util.toImmutableMap
import kotlin.jvm.JvmField

/**
 * A map of key/value pairs to support extensions.
 */
@Data
class Extras private constructor(
    private val data: Map<Key<*>, Any>,
) {

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: Key<T>): T? {
        return data[key] as T?
    }

    fun asMap(): Map<Key<*>, Any> {
        return data
    }

    fun newBuilder(): Builder {
        return Builder(this)
    }

    class Key<T>(
        val default: T,
    ) {
        /** Public to support static extensions. */
        companion object
    }

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

        @Suppress("UNCHECKED_CAST")
        fun setAll(extras: Extras) = apply {
            for ((key, value) in extras.data) {
                set(key as Key<Any>, value)
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

fun Extras?.orEmpty(): Extras {
    return this ?: Extras.EMPTY
}

fun <T> ImageRequest.getExtra(key: Extras.Key<T>): T {
    return extras[key] ?: defaults.extras[key] ?: key.default
}

fun <T> Options.getExtra(key: Extras.Key<T>): T {
    return extras[key] ?: key.default
}
