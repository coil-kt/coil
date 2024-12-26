package coil3.decode

import coil3.ColorImage
import coil3.Image
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlin.jvm.JvmField

/**
 * A [Decoder] that ignores the [SourceFetchResult] and always returns the [Image] returned by
 * [imageFactory].
 *
 * This is useful for skipping the decoding step, for instance when you only want to preload to disk
 * and do not want to decode the image into memory.
 */
@ExperimentalCoilApi
class BlackholeDecoder(
    private val imageFactory: () -> Image,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        return DecodeResult(
            image = imageFactory(),
            isSampled = false,
        )
    }

    class Factory(
        private val imageFactory: () -> Image = { EMPTY_IMAGE },
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ) = BlackholeDecoder(imageFactory)

        companion object {
            @JvmField val EMPTY_IMAGE: Image = ColorImage(color = 0x00000000)
        }
    }
}
