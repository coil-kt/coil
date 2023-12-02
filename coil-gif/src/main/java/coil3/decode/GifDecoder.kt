@file:Suppress("DEPRECATION")

package coil3.decode

import android.graphics.Bitmap
import android.graphics.Movie
import coil3.ImageLoader
import coil3.asCoilImage
import coil3.drawable.MovieDrawable
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.animatedTransformation
import coil3.request.animationEndCallback
import coil3.request.animationStartCallback
import coil3.request.bitmapConfig
import coil3.request.repeatCount
import coil3.util.animatable2CompatCallbackOf
import coil3.util.isHardware
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.runInterruptible
import okio.buffer

/**
 * A [Decoder] that uses [Movie] to decode GIFs.
 *
 * NOTE: Prefer using [ImageDecoderDecoder] on API 28 and above.
 *
 * @param enforceMinimumFrameDelay If true, rewrite a GIF's frame delay to a default value if
 *  it is below a threshold. See https://github.com/coil-kt/coil/issues/540 for more info.
 */
class GifDecoder @JvmOverloads constructor(
    private val source: ImageSource,
    private val options: Options,
    private val enforceMinimumFrameDelay: Boolean = true
) : Decoder {

    override suspend fun decode() = runInterruptible {
        val bufferedSource = if (enforceMinimumFrameDelay) {
            FrameDelayRewritingSource(source.source()).buffer()
        } else {
            source.source()
        }
        val movie: Movie? = bufferedSource.use { Movie.decodeStream(it.inputStream()) }

        check(movie != null && movie.width() > 0 && movie.height() > 0) { "Failed to decode GIF." }

        val drawable = MovieDrawable(
            movie = movie,
            config = when {
                movie.isOpaque && options.allowRgb565 -> Bitmap.Config.RGB_565
                options.bitmapConfig.isHardware -> Bitmap.Config.ARGB_8888
                else -> options.bitmapConfig
            },
            scale = options.scale
        )

        drawable.setRepeatCount(options.repeatCount)

        // Set the start and end animation callbacks if any one is supplied through the request.
        val onStart = options.animationStartCallback
        val onEnd = options.animationEndCallback
        if (onStart != null || onEnd != null) {
            drawable.registerAnimationCallback(animatable2CompatCallbackOf(onStart, onEnd))
        }

        // Set the animated transformation to be applied on each frame.
        drawable.setAnimatedTransformation(options.animatedTransformation)

        DecodeResult(
            image = drawable.asCoilImage(),
            isSampled = false
        )
    }

    @Poko
    class Factory @JvmOverloads constructor(
        val enforceMinimumFrameDelay: Boolean = true,
    ) : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!DecodeUtils.isGif(result.source.source())) return null
            return GifDecoder(result.source, options, enforceMinimumFrameDelay)
        }
    }

    companion object {
        const val REPEAT_COUNT_KEY = "coil#repeat_count"
        const val ANIMATED_TRANSFORMATION_KEY = "coil#animated_transformation"
        const val ANIMATION_START_CALLBACK_KEY = "coil#animation_start_callback"
        const val ANIMATION_END_CALLBACK_KEY = "coil#animation_end_callback"
    }
}
