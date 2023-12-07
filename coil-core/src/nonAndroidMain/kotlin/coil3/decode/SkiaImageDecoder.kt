package coil3.decode

import coil3.ImageLoader
import coil3.asCoilImage
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.util.makeFromImage
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

class SkiaImageDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        // https://github.com/JetBrains/skiko/issues/741
        val bytes = source.source().use { it.readByteArray() }
        val image = Image.makeFromEncoded(bytes)

        val isSampled: Boolean
        val bitmap: Bitmap
        try {
            bitmap = Bitmap.makeFromImage(image, options)
            bitmap.setImmutable()
            isSampled = bitmap.width < image.width || bitmap.height < image.height
        } finally {
            image.close()
        }

        return DecodeResult(
            image = bitmap.asCoilImage(),
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
