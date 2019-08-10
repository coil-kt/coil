@file:Suppress("unused")

package coil.request

/**
 * Represents the read/write policy for a cache source.
 *
 * @see Request.networkCachePolicy
 * @see Request.diskCachePolicy
 * @see Request.memoryCachePolicy
 */
enum class CachePolicy(
    val readEnabled: Boolean,
    val writeEnabled: Boolean
) {
    ENABLED(true, true),
    READ_ONLY(true, false),
    WRITE_ONLY(false, true),
    DISABLED(false, false)
}
