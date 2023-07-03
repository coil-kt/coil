package coil

import kotlin.jvm.JvmField

class Extras private constructor(
    private val data: Map<String, Any>,
) {

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String): T? {
        return data[key] as T?
    }

    fun newBuilder(): Builder {
        return Builder(this)
    }

    class Builder {
        private val data: MutableMap<String, Any>

        constructor() {
            data = mutableMapOf()
        }

        constructor(extras: Extras) {
            data = extras.data.toMutableMap()
        }

        fun put(key: String, value: Any?) = apply {
            if (value != null) {
                data[key] = value
            } else {
                data -= key
            }
        }

        fun build(): Extras {
            return Extras(data)
        }
    }

    companion object {
        @JvmField val EMPTY = Builder().build()
    }
}
