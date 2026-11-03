package coil3.decode

import coil3.ImageLoader
import coil3.asImage
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.util.allocDecodeBitmap
import coil3.util.makeFromImage
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.impl.use

class SkiaImageDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val data = tryMakeData(source)
        if (data != null) {
            return try {
                Codec.makeFromData(data).use { codec ->
                    val fmt = codec.encodedImageFormat
                    // Avoid allocating unscaled pixels on supported format (webp, jxl)
                    val scaleInplace = fmt == EncodedImageFormat.WEBP // TODO: Add JXL (subclass of SkScalingCodec)

                    val bitmap = if (scaleInplace) {
                        val dst = options.allocDecodeBitmap(codec.width, codec.height)
                        codec.readPixels(dst)
                        dst
                   } else {
                        val src = codec.readPixels()

                        // Avoid coping, SkImage take bitmap's pixel ref if not mutable
                        src.setImmutable()
                        val image = src.use { Image.makeFromBitmap(src) }
                        image.use { Bitmap.makeFromImage(image, options) }
                    }

                    bitmap.setImmutable()
                    val isSampled = bitmap.width < codec.width || bitmap.height < codec.height
                    DecodeResult(
                        image = bitmap.asImage(),
                        isSampled = isSampled,
                    )
                }
            } finally {
                data.close()
                source.close()
            }
        }

        // https://github.com/JetBrains/skiko/issues/741
        val bytes = source.source().use { it.readByteArray() }
        return decodeBitmap(options, bytes)
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

internal expect fun tryMakeData(source: ImageSource): Data?

internal expect suspend fun decodeBitmap(options: Options, bytes: ByteArray): DecodeResult

internal fun decodeBitmapSync(options: Options, bytes: ByteArray): DecodeResult {
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
        image = bitmap.asImage(),
        isSampled = isSampled,
    )
}
