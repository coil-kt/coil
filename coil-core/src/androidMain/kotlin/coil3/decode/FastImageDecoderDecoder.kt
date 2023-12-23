package coil3.decode

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.decodeDrawable
import androidx.core.util.component1
import androidx.core.util.component2
import coil3.ImageLoader
import coil3.asCoilImage
import coil3.drawable.ScaleDrawable
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.allowRgb565
import coil3.request.animatedTransformation
import coil3.request.animationEndCallback
import coil3.request.animationStartCallback
import coil3.request.bitmapConfig
import coil3.request.colorSpace
import coil3.request.premultipliedAlpha
import coil3.request.repeatCount
import coil3.toAndroidUri
import coil3.util.animatable2CallbackOf
import coil3.util.asPostProcessor
import coil3.util.heightPx
import coil3.util.isHardware
import coil3.util.widthPx
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

@RequiresApi(28)
class FastImageDecoderDecoder(
    private val source: ImageDecoder.Source,
    private val options: Options,
    private val parallelismLock: Semaphore = Semaphore(Int.MAX_VALUE),
) : Decoder {

    override suspend fun decode() = parallelismLock.withPermit {
        decodeInternal()
    }

    private suspend fun decodeInternal(): DecodeResult {
        var isSampled = false
        val drawable = runInterruptible {
            var imageDecoder: ImageDecoder? = null
            try {
                source.decodeDrawable { info, _ ->
                    // Capture the image decoder to manually close it later.
                    imageDecoder = this

                    // Configure the output image's size.
                    val (srcWidth, srcHeight) = info.size
                    val dstWidth = options.size.widthPx(options.scale) { srcWidth }
                    val dstHeight = options.size.heightPx(options.scale) { srcHeight }
                    if (srcWidth > 0 && srcHeight > 0 &&
                        (srcWidth != dstWidth || srcHeight != dstHeight)) {
                        val multiplier = DecodeUtils.computeSizeMultiplier(
                            srcWidth = srcWidth,
                            srcHeight = srcHeight,
                            dstWidth = dstWidth,
                            dstHeight = dstHeight,
                            scale = options.scale,
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

                    // Configure any other attributes.
                    configureImageDecoderProperties()
                }
            } finally {
                imageDecoder?.close()
            }
        }
        return DecodeResult(
            image = wrapDrawable(drawable).asCoilImage(),
            isSampled = isSampled,
        )
    }

    private fun ImageDecoder.configureImageDecoderProperties() {
        allocator = if (options.bitmapConfig.isHardware) {
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
        postProcessor = options.animatedTransformation?.asPostProcessor()
    }

    private suspend fun wrapDrawable(baseDrawable: Drawable): Drawable {
        if (baseDrawable !is AnimatedImageDrawable) {
            return baseDrawable
        }

        baseDrawable.repeatCount = options.repeatCount

        // Set the start and end animation callbacks if any one is supplied through the request.
        val onStart = options.animationStartCallback
        val onEnd = options.animationEndCallback
        if (onStart != null || onEnd != null) {
            // Animation callbacks must be set on the main thread.
            withContext(Dispatchers.Main.immediate) {
                baseDrawable.registerAnimationCallback(animatable2CallbackOf(onStart, onEnd))
            }
        }

        // Wrap AnimatedImageDrawable in a ScaleDrawable so it always scales to fill its bounds.
        return ScaleDrawable(baseDrawable, options.scale)
    }
}

class FastImageDecoderFactory @JvmOverloads constructor(
    maxParallelism: Int = DEFAULT_MAX_PARALLELISM,
) : Decoder.Factory {
    private val parallelismLock = Semaphore(maxParallelism)

    override fun create(
        result: SourceFetchResult,
        options: Options,
        imageLoader: ImageLoader,
    ): Decoder? {
        val mimeType = result.mimeType
        if (mimeType == "image/gif" && NeedRewriteGifSource) return null
        val source = result.source.fastImageDecoderSourceOrNull(options) ?: return null
        return FastImageDecoderDecoder(source, options, parallelismLock)
    }
}

@RequiresApi(28)
fun ImageSource.fastImageDecoderSourceOrNull(options: Options): ImageDecoder.Source? {
    val file = fileOrNull()
    if (file != null) {
        return ImageDecoder.createSource(file.toFile())
    }

    val metadata = metadata
    if (metadata is AssetMetadata) {
        return ImageDecoder.createSource(options.context.assets, metadata.filePath)
    }
    if (metadata is ContentMetadata) {
        return ImageDecoder.createSource(options.context.contentResolver, metadata.uri.toAndroidUri())
    }
    if (metadata is ResourceMetadata && metadata.packageName == options.context.packageName) {
        return ImageDecoder.createSource(options.context.resources, metadata.resId)
    }

    return null
}

private val NeedRewriteGifSource = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
private const val DEFAULT_MAX_PARALLELISM = 4
