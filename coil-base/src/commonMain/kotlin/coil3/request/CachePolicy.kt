package coil3.request

/**
 * Represents the read/write policy for a cache source.
 *
 * @see ImageRequest.memoryCachePolicy
 * @see ImageRequest.diskCachePolicy
 * @see ImageRequest.networkCachePolicy
 */
enum class CachePolicy(
    val readEnabled: Boolean,
    val writeEnabled: Boolean,
) {
    ENABLED(true, true),
    READ_ONLY(true, false),
    WRITE_ONLY(false, true),
    DISABLED(false, false),
}
