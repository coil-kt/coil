package coil3.decode

import coil3.ImageLoader
import coil3.bitmapFactoryExifOrientationPolicy

/**
 * Specifies the policy for handling the EXIF orientation flag.
 */
enum class ExifOrientationPolicy {

    /**
     * Ignore the EXIF orientation flag.
     */
    IGNORE,

    /**
     * Respect the EXIF orientation flag only for image formats that won't negatively affect
     * performance.
     *
     * This policy respects the EXIF orientation flag for the following MIME types:
     * - image/jpeg
     * - image/webp
     * - image/heic
     * - image/heif
     *
     * This is the default value for [ImageLoader.Builder.bitmapFactoryExifOrientationPolicy].
     */
    RESPECT_PERFORMANCE,

    /**
     * Respect the EXIF orientation flag for all supported formats.
     *
     * NOTE: This policy can potentially cause out of memory errors as certain image formats
     * (e.g. PNG) will be buffered entirely into memory while being decoded.
     */
    RESPECT_ALL,
}
