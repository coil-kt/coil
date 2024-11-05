package coil3.gif

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlin.time.TimeSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.plus
import okio.BufferedSource
import okio.use
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data

/**
 * A [Decoder] that uses Skia to decode animated images (GIF, WebP).
 *
 * @param bufferedFramesCount The number of frames to be pre-buffered before the animation
 * starts playing.
 */
class AnimatedSkiaImageDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val bufferedFramesCount: Int,
    private val timeSource: TimeSource,
) : Decoder {

    override suspend fun decode(): DecodeResult = coroutineScope {
        val bytes = source.source().use { it.readByteArray() }
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        DecodeResult(
            image = AnimatedSkiaImage(
                codec = codec,
                coroutineScope = this + Job(),
                timeSource = timeSource,
                bufferedFramesCount = bufferedFramesCount,
                animatedTransformation = options.animatedTransformation,
                onAnimationStart = options.animationStartCallback,
                onAnimationEnd = options.animationEndCallback,
            ),
            isSampled = false,
        )
    }

    class Factory(
        private val bufferedFramesCount: Int = DEFAULT_BUFFERED_FRAMES_COUNT,
        private val timeSource: TimeSource = TimeSource.Monotonic,
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return AnimatedSkiaImageDecoder(
                source = result.source,
                options = options,
                bufferedFramesCount = bufferedFramesCount,
                timeSource = timeSource,
            )
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            return DecodeUtils.isGif(source) || DecodeUtils.isAnimatedWebP(source)
        }
    }
}

private const val DEFAULT_BUFFERED_FRAMES_COUNT = 5
