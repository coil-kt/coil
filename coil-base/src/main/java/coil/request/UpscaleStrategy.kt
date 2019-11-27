package coil.request

/**
 * Used to determine if an image request should upscale.
 *
 * @see RequestBuilder.upscale
 */
enum class UpscaleStrategy {
    ENABLED,
    DISABLED,
    UNSPECIFIED
}
