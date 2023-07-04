package coil.request

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import coil.ImageLoader
import coil.target.ImageViewTarget
import coil.target.Target
import coil.util.DEFAULT_BITMAP_CONFIG
import coil.util.NULL_COLOR_SPACE
import coil.util.internalExtraKeyOf

/**
 * Convenience function to set [imageView] as the [Target].
 */
fun ImageRequest.Builder.target(imageView: ImageView) = target(ImageViewTarget(imageView))

// bitmapConfig

fun ImageRequest.Builder.bitmapConfig(config: Bitmap.Config) = apply {
    extra(bitmapConfigKey, config)
}

fun ImageLoader.Builder.bitmapConfig(config: Bitmap.Config) = apply {
    extra(bitmapConfigKey, config)
}

val ImageRequest.bitmapConfig: Bitmap.Config
    get() = extras.get(bitmapConfigKey)
        ?: defaults.extras.get(bitmapConfigKey)
        ?: bitmapConfigDefault

private val bitmapConfigKey = internalExtraKeyOf("bitmapConfig")
private val bitmapConfigDefault = DEFAULT_BITMAP_CONFIG

// colorSpace

/**
 * Set the preferred [ColorSpace].
 *
 * This is not guaranteed and a different color space may be used in some situations.
 */
@RequiresApi(26)
fun ImageRequest.Builder.colorSpace(colorSpace: ColorSpace) = apply {
    extra(colorSpaceKey, colorSpace)
}

@RequiresApi(26)
fun ImageLoader.Builder.colorSpace(colorSpace: ColorSpace) = apply {
    extra(colorSpaceKey, colorSpace)
}

val ImageRequest.colorSpace: ColorSpace?
    @RequiresApi(26) get() = extras.get(colorSpaceKey)
        ?: defaults.extras.get(colorSpaceKey)
        ?: colorSpaceDefault

private val colorSpaceKey = internalExtraKeyOf("colorSpace")
private val colorSpaceDefault = NULL_COLOR_SPACE

// premultipliedAlpha

/**
 * Enable/disable pre-multiplication of the color (RGB) channels of the decoded image by
 * the alpha channel.
 *
 * The default behavior is to enable pre-multiplication but in some environments it can be
 * necessary to disable this feature to leave the source pixels unmodified.
 */
fun ImageRequest.Builder.premultipliedAlpha(enable: Boolean) = apply {
    extra(premultipliedAlphaKey, enable)
}

fun ImageLoader.Builder.premultipliedAlpha(enable: Boolean) = apply {
    extra(premultipliedAlphaKey, enable)
}

val ImageRequest.premultipliedAlpha: Boolean
    get() = extras.get(premultipliedAlphaKey)
        ?: defaults.extras.get(premultipliedAlphaKey)
        ?: premultipliedAlphaDefault

private val premultipliedAlphaKey = internalExtraKeyOf("premultipliedAlpha")
private const val premultipliedAlphaDefault = true

// lifecycle

fun ImageRequest.Builder.lifecycle(owner: LifecycleOwner?) = lifecycle(owner?.lifecycle)

fun ImageRequest.Builder.lifecycle(lifecycle: Lifecycle?) = apply {
    extra(lifecycleKey, lifecycle)
}

val ImageRequest.lifecycle: Lifecycle?
    get() = extras.get(lifecycleKey)

private val lifecycleKey = internalExtraKeyOf("lifecycle")

// allowConversionToBitmap

fun ImageRequest.Builder.allowConversionToBitmap(enable: Boolean) = apply {
    extra(allowConversionToBitmapKey, enable)
}

fun ImageLoader.Builder.allowConversionToBitmap(enable: Boolean) = apply {
    extra(allowConversionToBitmapKey, enable)
}

val ImageRequest.allowConversionToBitmap: Boolean
    get() = extras.get(allowConversionToBitmapKey)
        ?: defaults.extras.get(allowConversionToBitmapKey)
        ?: allowConversionToBitmapDefault

private val allowConversionToBitmapKey = internalExtraKeyOf("allowConversionToBitmap")
private const val allowConversionToBitmapDefault = true

// allowHardware

fun ImageRequest.Builder.allowHardware(enable: Boolean) = apply {
    extra(allowHardwareKey, enable)
}

fun ImageLoader.Builder.allowHardware(enable: Boolean) = apply {
    extra(allowHardwareKey, enable)
}

val ImageRequest.allowHardware: Boolean
    get() = extras.get(allowHardwareKey)
        ?: defaults.extras.get(allowHardwareKey)
        ?: allowHardwareDefault

private val allowHardwareKey = internalExtraKeyOf("allowHardware")
private const val allowHardwareDefault = true

// allowRgb565

fun ImageRequest.Builder.allowRgb565(enable: Boolean) = apply {
    extra(allowRgb565Key, enable)
}

fun ImageLoader.Builder.allowRgb565(enable: Boolean) = apply {
    extra(allowRgb565Key, enable)
}

val ImageRequest.allowRgb565: Boolean
    get() = extras.get(allowRgb565Key)
        ?: defaults.extras.get(allowRgb565Key)
        ?: allowRgb565Default

private val allowRgb565Key = internalExtraKeyOf("allowRgb565")
private const val allowRgb565Default = false
