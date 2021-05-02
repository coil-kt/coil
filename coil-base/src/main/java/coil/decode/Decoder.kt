package coil.decode

import android.graphics.drawable.Drawable
import okio.BufferedSource

/**
 * Converts an [ImageSource] into a [Drawable].
 *
 * Use this interface to add support for custom file formats (e.g. GIF, SVG, TIFF, etc.).
 */
fun interface Decoder {

    /**
     * Decode [source] as a [Drawable].
     *
     * @param source The [ImageSource] to read from.
     * @param options A set of configuration options for this request.
     */
    suspend fun decode(source: ImageSource, options: Options): DecodeResult

    fun interface Factory {

        /**
         * Return a [Decoder] that can decode [source].
         * If this factory cannot create a [Decoder] for this [ImageSource], return null.
         *
         * Implementations **must not** consume the source, as this can cause calls to subsequent decoders to fail.
         *
         * Prefer using [BufferedSource.peek], [BufferedSource.rangeEquals], or other non-destructive methods to check
         * for the presence of header bytes or other markers. Implementations can also rely on [mimeType],
         * however it is not guaranteed to be accurate (e.g. a file that ends with .png, but is encoded as a .jpg).
         *
         * @param source The [ImageSource] to read from.
         * @param options A set of configuration options for this request.
         * @param mimeType An optional MIME type for the [source].
         */
        fun create(source: ImageSource, options: Options, mimeType: String?): Decoder?
    }
}
