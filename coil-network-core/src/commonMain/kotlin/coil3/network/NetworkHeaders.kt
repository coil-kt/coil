@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.network

import coil3.annotation.Data
import coil3.annotation.ExperimentalCoilApi
import kotlin.jvm.JvmField

/**
 * Represents a list of HTTP headers.
 */
@ExperimentalCoilApi
@Data
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

        operator fun set(key: String, value: String) = set(key, listOf(value))

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
        @JvmField
        val EMPTY = Builder().build()
    }
}
