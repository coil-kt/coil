@file:Suppress("unused")

package coil.request

/**
 * Represents the read/write policy for a cache source.
 *
 * @see ImageRequest.memoryCachePolicy
 * @see ImageRequest.diskCachePolicy
 * @see ImageRequest.networkCachePolicy
 */
public enum class CachePolicy(
    public val readEnabled: Boolean,
    public val writeEnabled: Boolean
) {
    ENABLED(true, true),
    READ_ONLY(true, false),
    WRITE_ONLY(false, true),
    DISABLED(false, false)
}
