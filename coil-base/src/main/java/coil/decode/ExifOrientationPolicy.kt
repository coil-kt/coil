package coil.decode

/**
 * Specifies the policy for handling the EXIF orientation flag.
 */
enum class ExifOrientationPolicy {

    /**
     * Ignore the EXIF orientation flag.
     */
    IGNORE,

    /**
     * Respect the EXIF orientation flag only for those formats for which the presence of
     * this flag is _typical_.
     *
     * It's guaranteed that this policy respects the EXIF orientation flag
     * for the following MIME types:
     * - image/jpeg
     * - image/webp
     * - image/heic
     * - image/heif
     */
    RESPECT_OPTIMAL,

    /**
     * Respect the EXIF orientation flag for all supported formats.
     */
    RESPECT_ALL
}
