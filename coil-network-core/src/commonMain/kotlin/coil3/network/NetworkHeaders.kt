package coil3.network

import coil3.annotation.Poko
import kotlin.jvm.JvmField

/**
 * A list of HTTP headers.
 */
@Poko
class NetworkHeaders private constructor(
    private val data: Map<String, List<String>>,
) {

    operator fun get(key: String): String? {
        return data[key.lowercase()]?.lastOrNull()
    }

    fun getAll(key: String): List<String> {
        return data[key.lowercase()].orEmpty()
    }

    fun asMap(): Map<String, List<String>> {
        return data
    }

    fun newBuilder() = Builder(this)

    class Builder {
        private val data: MutableMap<String, MutableList<String>>

        constructor() {
            data = mutableMapOf()
        }

        constructor(headers: NetworkHeaders) {
            data = headers.data.mapValuesTo(mutableMapOf()) { it.value.toMutableList() }
        }

        operator fun set(key: String, value: String) = apply {
            data[key.lowercase()] = mutableListOf(value)
        }

        operator fun set(key: String, values: List<String>) = apply {
            data[key.lowercase()] = values.toMutableList()
        }

        fun add(key: String, value: String) = apply {
            val values = data.getOrPut(key.lowercase()) { mutableListOf() }
            values += value
        }

        fun build(): NetworkHeaders {
            return NetworkHeaders(data.toMap())
        }
    }

    companion object {
        @JvmField val EMPTY = Builder().build()
    }
}
