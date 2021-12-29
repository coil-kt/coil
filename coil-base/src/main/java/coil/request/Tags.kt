package coil.request

import coil.util.toImmutableMap
import okhttp3.Request

/**
 * A map of custom objects. These are used to attach custom data to an [ImageRequest].
 *
 * Unlike [Parameters], [Tags] do not affect the memory cache key for a request. Tags are
 * also attached to any OkHttp [Request]s executed as part of an image request whereas parameters
 * are not.
 *
 * @see ImageRequest.tags
 */
class Tags private constructor(private val tags: Map<Class<*>, Any>) {

    /** Return the tag associated with [T]. */
    inline fun <reified T : Any> tag(): T? = tag(T::class.java)

    /** Return the tag associated with [type]. */
    fun <T : Any> tag(type: Class<out T>): T? = type.cast(tags[type])

    /** Get the underlying **immutable** map used by this instance. */
    fun asMap(): Map<Class<*>, Any> = tags

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Tags && tags == other.tags
    }

    override fun hashCode() = tags.hashCode()

    override fun toString() = "Tags(tags=$tags)"

    companion object {
        @JvmField val EMPTY = Tags(emptyMap())

        /** Create a new [Tags] from [tags]. */
        @JvmStatic
        fun from(tags: Map<Class<*>, Any>) = Tags(tags.toImmutableMap())
    }
}
