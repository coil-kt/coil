package coil3.decode

import coil3.ImageLoader
import coil3.bitmapFactoryExifOrientationPolicy
import coil3.util.MIME_TYPE_HEIC
import coil3.util.MIME_TYPE_HEIF
import coil3.util.MIME_TYPE_JPEG
import coil3.util.MIME_TYPE_WEBP

/**
 * Specifies the policy for handling the EXIF orientation flag.
 */
fun interface ExifOrientationPolicy {

    /**
     * Return true if the image should be normalized according to its EXIF data.
     */
    fun supports(mimeType: String?): Boolean

    companion object {
        /**
         * Ignore the EXIF orientation flag.
         */
        @JvmField val IGNORE = ExifOrientationPolicy { false }

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
        @JvmField val RESPECT_PERFORMANCE = ExifOrientationPolicy { mimeType ->
            mimeType != null && (
                mimeType == MIME_TYPE_JPEG ||
                    mimeType == MIME_TYPE_WEBP ||
                    mimeType == MIME_TYPE_HEIC ||
                    mimeType == MIME_TYPE_HEIF
                )
        }

        /**
         * Respect the EXIF orientation flag for all supported formats.
         *
         * NOTE: This policy can potentially cause out of memory errors as certain image formats
         * (e.g. PNG) will be buffered entirely into memory while being decoded.
         */
        @JvmField val RESPECT_ALL = ExifOrientationPolicy { true }
    }
}
