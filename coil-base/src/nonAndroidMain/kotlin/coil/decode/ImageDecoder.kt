package coil.decode

import org.jetbrains.skia.Image as SkiaImage
import coil.ImageLoader
import coil.asCoilImage
import coil.fetch.SourceFetchResult
import coil.request.Options
import okio.use

class ImageDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        // https://github.com/JetBrains/skiko/issues/741
        val image = SkiaImage.makeFromEncoded(source.source().use { it.readByteArray() })
        return DecodeResult(
            image = image.asCoilImage(),
            isSampled = false,
        )
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder {
            return ImageDecoder(result.source, options)
        }
    }
}
