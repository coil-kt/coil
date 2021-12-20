package coil.decode

import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import coil.ImageLoader
import coil.drawable.ScaleDrawable
import coil.fetch.SourceResult
import coil.request.Options
import coil.request.animatedTransformation
import coil.request.animationEndCallback
import coil.request.animationStartCallback
import coil.request.repeatCount
import coil.size.pxOrElse
import coil.util.animatable2CallbackOf
import coil.util.asPostProcessor
import coil.util.isHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okio.BufferedSource
import okio.buffer
import java.nio.ByteBuffer
import kotlin.math.roundToInt

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
    private val enforceMinimumFrameDelay: Boolean = true
) : Decoder {

    override suspend fun decode(): DecodeResult {
        var isSampled = false
        val baseDrawable = runInterruptible {
            val imageSource = if (enforceMinimumFrameDelay && DecodeUtils.isGif(source.source())) {
                ImageSource(FrameDelayRewritingSource(source.source()).buffer(), options.context)
            } else {
                source
            }
            imageSource.use {
                imageSource.toImageDecoderSource().decodeDrawable { info, _ ->
                    val srcWidth = info.size.width.coerceAtLeast(1)
                    val srcHeight = info.size.height.coerceAtLeast(1)
                    val dstWidth = options.size.width.pxOrElse { srcWidth }
                    val dstHeight = options.size.height.pxOrElse { srcHeight }
                    if (srcWidth != dstWidth || srcHeight != dstHeight) {
                        val multiplier = DecodeUtils.computeSizeMultiplier(
                            srcWidth = srcWidth,
                            srcHeight = srcHeight,
                            dstWidth = dstWidth,
                            dstHeight = dstHeight,
                            scale = options.scale
                        )

                        // Set the target size if the image is larger than the requested dimensions
                        // or the request requires exact dimensions.
                        isSampled = multiplier < 1
                        if (isSampled || !options.allowInexactSize) {
                            val targetWidth = (multiplier * srcWidth).roundToInt()
                            val targetHeight = (multiplier * srcHeight).roundToInt()
                            setTargetSize(targetWidth, targetHeight)
                        }
                    }

                    allocator = if (options.config.isHardware) {
                        ImageDecoder.ALLOCATOR_HARDWARE
                    } else {
                        ImageDecoder.ALLOCATOR_SOFTWARE
                    }

                    memorySizePolicy = if (options.allowRgb565) {
                        ImageDecoder.MEMORY_POLICY_LOW_RAM
                    } else {
                        ImageDecoder.MEMORY_POLICY_DEFAULT
                    }

                    if (options.colorSpace != null) {
                        setTargetColorSpace(options.colorSpace)
                    }

                    isUnpremultipliedRequired = !options.premultipliedAlpha

                    postProcessor = options.parameters.animatedTransformation()?.asPostProcessor()
                }
            }
        }

        val drawable = if (baseDrawable is AnimatedImageDrawable) {
            baseDrawable.repeatCount = options.parameters.repeatCount() ?: AnimatedImageDrawable.REPEAT_INFINITE

            // Set the start and end animation callbacks if any one is supplied through the request.
            val onStart = options.parameters.animationStartCallback()
            val onEnd = options.parameters.animationEndCallback()
            if (onStart != null || onEnd != null) {
                // Animation callbacks must be set on the main thread.
                withContext(Dispatchers.Main.immediate) {
                    baseDrawable.registerAnimationCallback(animatable2CallbackOf(onStart, onEnd))
                }
            }

            // Wrap AnimatedImageDrawable in a ScaleDrawable so it always scales to fill its bounds.
            ScaleDrawable(baseDrawable, options.scale)
        } else {
            baseDrawable
        }

        return DecodeResult(
            drawable = drawable,
            isSampled = isSampled
        )
    }

    private fun ImageSource.toImageDecoderSource(): ImageDecoder.Source {
        val file = fileOrNull()
        val uri = uriOrNull()?.takeIf { it.scheme in SUPPORTED_SCHEMES }
        return when {
            file != null -> ImageDecoder.createSource(file)
            uri != null -> {
                val assetFileName = uri.assetFileNameOrNull()
                if (assetFileName != null) {
                    ImageDecoder.createSource(options.context.assets, assetFileName)
                } else {
                    ImageDecoder.createSource(options.context.contentResolver, uri)
                }
            }
            // https://issuetracker.google.com/issues/139371066
            SDK_INT < 30 -> ImageDecoder.createSource(file())
            else -> ImageDecoder.createSource(ByteBuffer.wrap(source().readByteArray()))
        }
    }

    /** Modified from `AssetUriFetcher`. */
    private fun Uri.assetFileNameOrNull(): String? {
        if (scheme == SCHEME_FILE && pathSegments.firstOrNull() == "android_asset") {
            return pathSegments.drop(1).lastOrNull()
        } else {
            return null
        }
    }

    @RequiresApi(28)
    class Factory @JvmOverloads constructor(
        private val enforceMinimumFrameDelay: Boolean = true
    ) : Decoder.Factory {

        override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
            if (!isApplicable(result.source.source())) return null
            return ImageDecoderDecoder(result.source, options, enforceMinimumFrameDelay)
        }

        private fun isApplicable(source: BufferedSource): Boolean {
            return DecodeUtils.isGif(source) ||
                DecodeUtils.isAnimatedWebP(source) ||
                (SDK_INT >= 30 && DecodeUtils.isAnimatedHeif(source))
        }

        override fun equals(other: Any?) = other is Factory

        override fun hashCode() = javaClass.hashCode()
    }

    private companion object {
        val SUPPORTED_SCHEMES = arrayOf(SCHEME_CONTENT, SCHEME_FILE)
    }
}
