package coil.decode

import coil.ImageLoader
import coil.fetch.SourceFetchResult
import coil.request.Options
import coil.toCoilImage
import coil.util.makeFromImage
import coil.util.isStatic
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image
import org.jetbrains.skia.impl.use

class SkiaImageDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        // https://github.com/JetBrains/skiko/issues/741
        val bytes = source.source().use { it.readByteArray() }
        val isStatic = Data.makeFromBytes(bytes).use { data ->
            Codec.makeFromData(data).use { codec -> codec.isStatic }
        }
        var isSampled = false
        var image = Image.makeFromEncoded(bytes)
        if (isStatic) {
            val bitmap = Bitmap.makeFromImage(image, options.size, options.scale)
            bitmap.setImmutable()

            isSampled = bitmap.width < image.width || bitmap.height < image.height
            image = Image.makeFromBitmap(bitmap)
        }
        return DecodeResult(
            image = image.toCoilImage(shareable = isStatic),
            isSampled = isSampled,
        )
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder {
            return SkiaImageDecoder(result.source, options)
        }
    }
}
