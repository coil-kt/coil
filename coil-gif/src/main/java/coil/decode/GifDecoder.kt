@file:Suppress("DEPRECATION")

package coil.decode

import android.graphics.Bitmap
import android.graphics.Movie
import coil.ImageLoader
import coil.drawable.MovieDrawable
import coil.fetch.SourceResult
import coil.request.Options
import coil.request.animatedTransformation
import coil.request.animationEndCallback
import coil.request.animationStartCallback
import coil.request.repeatCount
import coil.util.animatable2CompatCallbackOf
import coil.util.isHardware
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
                options.config.isHardware -> Bitmap.Config.ARGB_8888
                else -> options.config
            },
            scale = options.scale
        )

        drawable.setRepeatCount(options.parameters.repeatCount() ?: MovieDrawable.REPEAT_INFINITE)

        // Set the start and end animation callbacks if any one is supplied through the request.
        val onStart = options.parameters.animationStartCallback()
        val onEnd = options.parameters.animationEndCallback()
        if (onStart != null || onEnd != null) {
            drawable.registerAnimationCallback(animatable2CompatCallbackOf(onStart, onEnd))
        }

        // Set the animated transformation to be applied on each frame.
        drawable.setAnimatedTransformation(options.parameters.animatedTransformation())

        DecodeResult(
            drawable = drawable,
            isSampled = false
        )
    }

    class Factory @JvmOverloads constructor(
        private val enforceMinimumFrameDelay: Boolean = true
    ) : Decoder.Factory {

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (!DecodeUtils.isGif(result.source.source())) return null
            return GifDecoder(result.source, options, enforceMinimumFrameDelay)
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    companion object {
        const val REPEAT_COUNT_KEY = "coil#repeat_count"
        const val ANIMATED_TRANSFORMATION_KEY = "coil#animated_transformation"
        const val ANIMATION_START_CALLBACK_KEY = "coil#animation_start_callback"
        const val ANIMATION_END_CALLBACK_KEY = "coil#animation_end_callback"
    }
}
