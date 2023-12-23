package coil3.decode

import android.graphics.ImageDecoder
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import coil3.ImageLoader
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import java.nio.ByteBuffer
import okio.BufferedSource
import okio.buffer

/**
 * A [Decoder] that uses [ImageDecoder] to decode GIFs, animated WebPs, and animated HEIFs.
 *
 * NOTE: Animated HEIF files are only supported on API 30 and above.
 *
 * @param enforceMinimumFrameDelay If true, rewrite a GIF's frame delay to a default value if
 *  it is below a threshold. See https://github.com/coil-kt/coil/issues/540 for more info.
 */
@RequiresApi(28)
class ImageDecoderDecoder @JvmOverloads constructor(
    private val source: ImageSource,
    private val options: Options,
    private val enforceMinimumFrameDelay: Boolean = true,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        return wrapImageSource(source).use { wrappedSource ->
            FastImageDecoderDecoder(wrappedSource.toImageDecoderSource(), options).decode()
        }
    }

    private fun wrapImageSource(source: ImageSource): ImageSource {
        return if (enforceMinimumFrameDelay && DecodeUtils.isGif(source.source())) {
            // Wrap the source to rewrite its frame delay as it's read.
            ImageSource(
                source = FrameDelayRewritingSource(source.source()).buffer(),
                fileSystem = options.fileSystem,
            )
        } else {
            source
        }
    }

    private fun ImageSource.toImageDecoderSource(): ImageDecoder.Source {
        val fastSource = fastImageDecoderSourceOrNull(options)
        if (fastSource != null) return fastSource

        return when {
            SDK_INT >= 31 -> ImageDecoder.createSource(source().readByteArray())
            SDK_INT == 30 -> ImageDecoder.createSource(ByteBuffer.wrap(source().readByteArray()))
            // https://issuetracker.google.com/issues/139371066
            else -> ImageDecoder.createSource(file().toFile())
        }
    }

    class Factory @JvmOverloads constructor(
        val enforceMinimumFrameDelay: Boolean = true,
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return ImageDecoderDecoder(result.source, options, enforceMinimumFrameDelay)
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            return DecodeUtils.isGif(source) ||
                DecodeUtils.isAnimatedWebP(source) ||
                (SDK_INT >= 30 && DecodeUtils.isAnimatedHeif(source))
        }
    }
}
