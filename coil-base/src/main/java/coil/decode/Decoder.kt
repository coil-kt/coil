package coil.decode

import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.BufferedSource

/**
 * A [Decoder] converts a [SourceResult] into a [Drawable].
 *
 * Use this interface to add support for custom file formats (e.g. GIF, SVG, TIFF, etc.).
 */
fun interface Decoder {

    /**
     * Decode the [SourceResult] provided by [Factory.create] or return 'null' to delegate to the
     * next [Decoder] in the component registry.
     */
    suspend fun decode(): DecodeResult?

    fun interface Factory {

        /**
         * Return a [Decoder] that can decode [result] or 'null' if this factory cannot
         * create a decoder for the source.
         *
         * Implementations **must not** consume [result]'s [ImageSource], as this can cause calls
         * to subsequent decoders to fail. [ImageSource]s should only be consumed in [decode].
         *
         * Prefer using [BufferedSource.peek], [BufferedSource.rangeEquals], or other
         * non-destructive methods to check for the presence of header bytes or other markers.
         * Implementations can also rely on [SourceResult.mimeType], however it is not guaranteed
         * to be accurate (e.g. a file that ends with .png, but is encoded as a .jpg).
         *
         * @param result The result from the [Fetcher].
         * @param options A set of configuration options for this request.
         * @param imageLoader The [ImageLoader] that's executing this request.
         */
        fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder?
    }
}
