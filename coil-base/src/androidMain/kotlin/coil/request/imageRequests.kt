package coil.request

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import coil.Extras
import coil.ImageLoader
import coil.getOrDefault
import coil.target.ImageViewTarget
import coil.target.Target
import coil.util.DEFAULT_BITMAP_CONFIG
import coil.util.NULL_COLOR_SPACE

/**
 * Convenience function to set [imageView] as the [Target].
 */
fun ImageRequest.Builder.target(imageView: ImageView) = target(ImageViewTarget(imageView))

// bitmapConfig

fun ImageRequest.Builder.bitmapConfig(config: Bitmap.Config) = apply {
    extras[bitmapConfigKey] = config
}

fun ImageLoader.Builder.bitmapConfig(config: Bitmap.Config) = apply {
    extras[bitmapConfigKey] = config
}

val ImageRequest.bitmapConfig: Bitmap.Config
    get() = extras.getOrDefault(bitmapConfigKey, defaults.extras)

val Options.bitmapConfig: Bitmap.Config
    get() = extras.getOrDefault(bitmapConfigKey)

private val bitmapConfigKey = Extras.Key(default = DEFAULT_BITMAP_CONFIG)

// colorSpace

/**
 * Set the preferred [ColorSpace].
 *
 * This is not guaranteed and a different color space may be used in some situations.
 */
@RequiresApi(26)
fun ImageRequest.Builder.colorSpace(colorSpace: ColorSpace) = apply {
    extras[colorSpaceKey] = colorSpace
}

@RequiresApi(26)
fun ImageLoader.Builder.colorSpace(colorSpace: ColorSpace) = apply {
    extras[colorSpaceKey] = colorSpace
}

val ImageRequest.colorSpace: ColorSpace?
    @RequiresApi(26) get() = extras.getOrDefault(colorSpaceKey, defaults.extras)

val Options.colorSpace: ColorSpace?
    @RequiresApi(26) get() = extras.getOrDefault(colorSpaceKey)

private val colorSpaceKey = Extras.Key(default = NULL_COLOR_SPACE)

// premultipliedAlpha

/**
 * Enable/disable pre-multiplication of the color (RGB) channels of the decoded image by
 * the alpha channel.
 *
 * The default behavior is to enable pre-multiplication but in some environments it can be
 * necessary to disable this feature to leave the source pixels unmodified.
 */
fun ImageRequest.Builder.premultipliedAlpha(enable: Boolean) = apply {
    extras[premultipliedAlphaKey] = enable
}

fun ImageLoader.Builder.premultipliedAlpha(enable: Boolean) = apply {
    extras[premultipliedAlphaKey] = enable
}

val ImageRequest.premultipliedAlpha: Boolean
    get() = extras.getOrDefault(premultipliedAlphaKey, defaults.extras)

val Options.premultipliedAlpha: Boolean
    get() = extras.getOrDefault(premultipliedAlphaKey)

private val premultipliedAlphaKey = Extras.Key(default = true)

// lifecycle

fun ImageRequest.Builder.lifecycle(owner: LifecycleOwner?) = lifecycle(owner?.lifecycle)

fun ImageRequest.Builder.lifecycle(lifecycle: Lifecycle?) = apply {
    extras[lifecycleKey] = lifecycle
}

val ImageRequest.lifecycle: Lifecycle?
    get() = extras.getOrDefault(lifecycleKey, defaults.extras)

val Options.lifecycle: Lifecycle?
    get() = extras.getOrDefault(lifecycleKey)

private val lifecycleKey = Extras.Key<Lifecycle?>(default = null)

// allowConversionToBitmap

fun ImageRequest.Builder.allowConversionToBitmap(enable: Boolean) = apply {
    extras[allowConversionToBitmapKey] = enable
}

fun ImageLoader.Builder.allowConversionToBitmap(enable: Boolean) = apply {
    extras[allowConversionToBitmapKey] = enable
}

val ImageRequest.allowConversionToBitmap: Boolean
    get() = extras.getOrDefault(allowConversionToBitmapKey, defaults.extras)

val Options.allowConversionToBitmap: Boolean
    get() = extras.getOrDefault(allowConversionToBitmapKey)

private val allowConversionToBitmapKey = Extras.Key(default = true)

// allowHardware

fun ImageRequest.Builder.allowHardware(enable: Boolean) = apply {
    extras[allowHardwareKey] = enable
}

fun ImageLoader.Builder.allowHardware(enable: Boolean) = apply {
    extras[allowHardwareKey] = enable
}

val ImageRequest.allowHardware: Boolean
    get() = extras.getOrDefault(allowHardwareKey, defaults.extras)

val Options.allowHardware: Boolean
    get() = extras.getOrDefault(allowHardwareKey)

private val allowHardwareKey = Extras.Key(default = true)

// allowRgb565

fun ImageRequest.Builder.allowRgb565(enable: Boolean) = apply {
    extras[allowRgb565Key] = enable
}

fun ImageLoader.Builder.allowRgb565(enable: Boolean) = apply {
    extras[allowRgb565Key] = enable
}

val ImageRequest.allowRgb565: Boolean
    get() = extras.getOrDefault(allowRgb565Key, defaults.extras)

val Options.allowRgb565: Boolean
    get() = extras.getOrDefault(allowRgb565Key)

private val allowRgb565Key = Extras.Key(default = false)
