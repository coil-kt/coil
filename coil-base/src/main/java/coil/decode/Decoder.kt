package coil.decode

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import coil.bitmap.BitmapPool
import coil.size.Size
import okio.BufferedSource

/**
 * Converts a [BufferedSource] into a [Drawable].
 *
 * Use this interface to add support for custom file formats (e.g. GIF, SVG, TIFF, etc.).
 */
interface Decoder {

    /**
     * Return true if this decoder supports decoding [source].
     *
     * Implementations **must not** consume the source, as this can cause subsequent calls to [handles] and [decode] to fail.
     *
     * Prefer using [BufferedSource.peek], [BufferedSource.rangeEquals], or other non-destructive methods to check
     * for the presence of header bytes or other markers. Implementations can also rely on [mimeType],
     * however it is not guaranteed to be accurate (e.g. a file that ends with .png, but is encoded as a .jpg).
     *
     * @param source The [BufferedSource] to read from.
     * @param mimeType An optional MIME type for the [source].
     */
    fun handles(source: BufferedSource, mimeType: String?): Boolean

    /**
     * Decode [source] as a [Drawable].
     *
     * NOTE: Implementations are responsible for closing [source] when finished with it.
     *
     * @param pool A [BitmapPool] which can be used to request [Bitmap] instances.
     * @param source The [BufferedSource] to read from.
     * @param size The requested dimensions for the image.
     * @param options A set of configuration options for this request.
     */
    suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult
}
