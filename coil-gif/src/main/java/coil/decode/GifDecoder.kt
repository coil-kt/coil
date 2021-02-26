@file:Suppress("DEPRECATION", "unused")

package coil.decode

import android.graphics.Bitmap
import android.graphics.Movie
import android.graphics.drawable.Drawable
import android.os.Build.VERSION.SDK_INT
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import coil.bitmap.BitmapPool
import coil.drawable.MovieDrawable
import coil.request.animatedTransformation
import coil.request.animationEndCallback
import coil.request.animationStartCallback
import coil.request.repeatCount
import coil.size.Size
import okio.BufferedSource
import okio.buffer

/**
 * A [Decoder] that uses [Movie] to decode GIFs.
 *
 * NOTE: Prefer using [ImageDecoderDecoder] on API 28 and above.
 */
class GifDecoder : Decoder {

    override fun handles(source: BufferedSource, mimeType: String?) = DecodeUtils.isGif(source)

    override suspend fun decode(
        pool: BitmapPool,
        source: BufferedSource,
        size: Size,
        options: Options
    ): DecodeResult = withInterruptibleSource(source) { interruptibleSource ->
        val bufferedSource = interruptibleSource.buffer()
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
                SDK_INT >= 26 && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
                else -> options.config
            },
            scale = options.scale
        )

        drawable.setRepeatCount(options.parameters.repeatCount() ?: MovieDrawable.REPEAT_INFINITE)

        // Set the start and end animation callbacks if any one is supplied through the request.
        if (options.parameters.animationStartCallback() != null || options.parameters.animationEndCallback() != null) {
            drawable.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationStart(drawable: Drawable?) {
                    options.parameters.animationStartCallback()?.invoke()
                }

                override fun onAnimationEnd(drawable: Drawable?) {
                    options.parameters.animationEndCallback()?.invoke()
                }
            })
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
