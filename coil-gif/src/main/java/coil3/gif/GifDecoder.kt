@file:Suppress("DEPRECATION", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package coil3.gif

import android.graphics.Bitmap
import android.graphics.Movie
import coil3.ImageLoader
import coil3.asCoilImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.gif.internal.animatable2CompatCallbackOf
import coil3.gif.internal.maybeWrapImageSourceToRewriteFrameDelay
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.util.isHardware
import kotlinx.coroutines.runInterruptible

/**
 * A [Decoder] that uses [Movie] to decode GIFs.
 *
 * NOTE: Prefer using [AnimatedImageDecoder] on API 28 and above.
 *
 * @param enforceMinimumFrameDelay If true, rewrite a GIF's frame delay to a default value if
 *  it is below a threshold. See https://github.com/coil-kt/coil/issues/540 for more info.
 */
class GifDecoder @JvmOverloads constructor(
    private val source: ImageSource,
    private val options: Options,
    private val enforceMinimumFrameDelay: Boolean = true,
) : Decoder {

    override suspend fun decode() = runInterruptible {
        val source = maybeWrapImageSourceToRewriteFrameDelay(source, enforceMinimumFrameDelay)
        val movie: Movie? = source.use { Movie.decodeStream(it.source().inputStream()) }

        check(movie != null && movie.width() > 0 && movie.height() > 0) { "Failed to decode GIF." }

        val drawable = MovieDrawable(
            movie = movie,
            config = when {
                movie.isOpaque && options.allowRgb565 -> Bitmap.Config.RGB_565
                options.bitmapConfig.isHardware -> Bitmap.Config.ARGB_8888
                else -> options.bitmapConfig
            },
            scale = options.scale,
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
            isSampled = false,
        )
    }

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
