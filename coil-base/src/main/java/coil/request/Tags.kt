package coil.request

import coil.util.toImmutableMap

/**
 * A map of custom objects.
 */
class Tags private constructor(private val tags: Map<Class<*>, Any>) {

    inline fun <reified T : Any> tag(): T? = tag(T::class.java)

    fun <T : Any> tag(type: Class<out T>): T? = type.cast(tags[type])

    fun asMap(): Map<Class<*>, Any> = tags

    companion object {
        @JvmField val EMPTY = Tags(emptyMap())

        @JvmStatic
        fun from(tags: Map<Class<*>, Any>) = Tags(tags.toImmutableMap())
    }
}
