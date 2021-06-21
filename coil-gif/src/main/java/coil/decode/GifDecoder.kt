@file:Suppress("DEPRECATION", "unused")

package coil.decode

import android.graphics.Bitmap
import android.graphics.Movie
import android.os.Build.VERSION.SDK_INT
import coil.bitmap.BitmapPool
import coil.drawable.MovieDrawable
import coil.request.animatedTransformation
import coil.request.animationEndCallback
import coil.request.animationStartCallback
import coil.request.repeatCount
import coil.size.Size
import coil.util.animatable2CompatCallbackOf
import coil.util.isHardware
import okio.BufferedSource
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
    private val enforceMinimumFrameDelay: Boolean = false
) : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?) = DecodeUtils.isGif(source)

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult = withInterruptibleSource(source) { interruptibleSource ->
        val bufferedSource = if (enforceMinimumFrameDelay) {
            FrameDelayRewritingSource(interruptibleSource).buffer()
        } else {
            interruptibleSource.buffer()
        }
        val movie: Movie? = bufferedSource.use {
            if (SDK_INT >= 19) {
                Movie.decodeStream(it.inputStream())
            } else {
                // Movie requires an InputStream to resettable on API 18 and below.
                // Read the data as a ByteArray to work around this.
                it.readByteArray().let { bytes -> Movie.decodeByteArray(bytes, 0, bytes.size) }
            }
        }

        check(movie != null && movie.width() > 0 && movie.height() > 0) { "Failed to decode GIF." }

        val drawable = MovieDrawable(
            movie = movie,
            pool = pool,
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

    companion object {
        const val REPEAT_COUNT_KEY = "coil#repeat_count"
        const val ANIMATED_TRANSFORMATION_KEY = "coil#animated_transformation"
        const val ANIMATION_START_CALLBACK_KEY = "coil#animation_start_callback"
        const val ANIMATION_END_CALLBACK_KEY = "coil#animation_end_callback"
    }
}
