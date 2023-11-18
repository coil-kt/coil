package coil

import coil.request.ImageRequest
import coil.request.Options
import coil.util.toImmutableMap
import dev.drewhamilton.poko.Poko
import kotlin.jvm.JvmField

@Poko
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

fun <T> ImageRequest.getExtra(key: Extras.Key<T>): T {
    return extras[key] ?: defaults.extras[key] ?: key.default
}

fun <T> Options.getExtra(key: Extras.Key<T>): T {
    return extras[key] ?: key.default
}
