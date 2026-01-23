package coil3.gif

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
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
 * starts playing. Must be >= 1.
 */
class AnimatedSkiaImageDecoder(
    private val source: ImageSource,
    private val options: Options,
    private val bufferedFramesCount: Int,
    private val timeSource: TimeSource,
    private val decoderCoroutineContext: CoroutineContext = EmptyCoroutineContext,
) : Decoder {

    init {
        // Comment #4: Add validation in decoder
        require(bufferedFramesCount >= 1) { "bufferedFramesCount must be >= 1" }
    }

    override suspend fun decode(): DecodeResult = coroutineScope {
        val bytes = source.source().use { it.readByteArray() }
        val codec = Codec.makeFromData(Data.makeFromBytes(bytes))
        DecodeResult(
            image = AnimatedSkiaImage(
                codec = codec,
                coroutineScope = this + Job(),
                timeSource = timeSource,
                decoderCoroutineContext = decoderCoroutineContext,
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
        private val decoderCoroutineContext: CoroutineContext = EmptyCoroutineContext,
    ) : Decoder.Factory {

        init {
            // Comment #4: Add validation in factory
            require(bufferedFramesCount >= 1) { "bufferedFramesCount must be >= 1" }
        }

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
                decoderCoroutineContext = decoderCoroutineContext,
            )
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            return DecodeUtils.isGif(source) || DecodeUtils.isAnimatedWebP(source)
        }
    }

    companion object {
        // Comment #4: Move constant to companion and make public
        const val DEFAULT_BUFFERED_FRAMES_COUNT = 2
    }
}
