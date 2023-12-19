package coil3.request

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import coil3.Extras
import coil3.ImageLoader
import coil3.asCoilImage
import coil3.getExtra
import coil3.target.ImageViewTarget
import coil3.transform.Transformation
import coil3.transition.CrossfadeTransition
import coil3.transition.Transition
import coil3.util.DEFAULT_BITMAP_CONFIG
import coil3.util.NULL_COLOR_SPACE
import coil3.util.getDrawableCompat
import coil3.util.toImmutableList

fun ImageRequest.Builder.target(imageView: ImageView) =
    target(ImageViewTarget(imageView))

fun ImageRequest.Builder.placeholder(@DrawableRes drawableResId: Int) =
    placeholder { it.context.getDrawableCompat(drawableResId).asCoilImage() }

fun ImageRequest.Builder.placeholder(drawable: Drawable?) =
    placeholder(drawable?.asCoilImage())

fun ImageRequest.Builder.error(@DrawableRes drawableResId: Int) =
    error { it.context.getDrawableCompat(drawableResId).asCoilImage() }

fun ImageRequest.Builder.error(drawable: Drawable?) =
    error(drawable?.asCoilImage())

fun ImageRequest.Builder.fallback(@DrawableRes drawableResId: Int) =
    fallback { it.context.getDrawableCompat(drawableResId).asCoilImage() }

fun ImageRequest.Builder.fallback(drawable: Drawable?) =
    fallback(drawable?.asCoilImage())

// region transformations

/**
 * Set [Transformation]s to be applied to the output image.
 */
fun ImageRequest.Builder.transformations(vararg transformations: Transformation) =
    transformations(transformations.toList())

fun ImageRequest.Builder.transformations(transformations: List<Transformation>) = apply {
    extras[transformationsKey] = transformations.toImmutableList()
}

val ImageRequest.transformations: List<Transformation>
    get() = getExtra(transformationsKey)

val Options.transformations: List<Transformation>
    get() = getExtra(transformationsKey)

val Extras.Key.Companion.transformations: Extras.Key<List<Transformation>>
    get() = transformationsKey

private val transformationsKey = Extras.Key<List<Transformation>>(default = emptyList())

// endregion
// region crossfade

/**
 * Enable a crossfade animation when a request completes successfully.
 */
actual fun ImageRequest.Builder.crossfade(enable: Boolean) =
    crossfade(if (enable) DEFAULT_CROSSFADE_MILLIS else 0)

actual fun ImageRequest.Builder.crossfade(durationMillis: Int): ImageRequest.Builder {
    return transitionFactory(newCrossfadeTransitionFactory(durationMillis))
}

actual fun ImageLoader.Builder.crossfade(enable: Boolean) =
    crossfade(if (enable) DEFAULT_CROSSFADE_MILLIS else 0)

actual fun ImageLoader.Builder.crossfade(durationMillis: Int): ImageLoader.Builder {
    return transitionFactory(newCrossfadeTransitionFactory(durationMillis))
}

private fun newCrossfadeTransitionFactory(durationMillis: Int): Transition.Factory {
    return if (durationMillis > 0) {
        CrossfadeTransition.Factory(durationMillis)
    } else {
        Transition.Factory.NONE
    }
}

actual val ImageRequest.crossfadeMillis: Int
    get() = (transitionFactory as? CrossfadeTransition.Factory)?.durationMillis ?: 0

// endregion
// region transitionFactory

/**
 * Set the [Transition.Factory] that's started when an image result is applied to a target.
 */
fun ImageRequest.Builder.transitionFactory(factory: Transition.Factory) = apply {
    extras[transitionFactoryKey] = factory
}

fun ImageLoader.Builder.transitionFactory(factory: Transition.Factory) = apply {
    extras[transitionFactoryKey] = factory
}

val ImageRequest.transitionFactory: Transition.Factory
    get() = getExtra(transitionFactoryKey)

val Extras.Key.Companion.transitionFactory: Extras.Key<Transition.Factory>
    get() = transitionFactoryKey

private val transitionFactoryKey = Extras.Key(default = Transition.Factory.NONE)

// endregion
// region bitmapConfig

/**
 * Set the preferred [Bitmap.Config].
 *
 * This is not guaranteed and a different bitmap config may be used in some situations.
 */
fun ImageRequest.Builder.bitmapConfig(config: Bitmap.Config) = apply {
    extras[bitmapConfigKey] = config
}

fun ImageLoader.Builder.bitmapConfig(config: Bitmap.Config) = apply {
    extras[bitmapConfigKey] = config
}

val ImageRequest.bitmapConfig: Bitmap.Config
    get() = getExtra(bitmapConfigKey)

val Options.bitmapConfig: Bitmap.Config
    get() = getExtra(bitmapConfigKey)

val Extras.Key.Companion.bitmapConfig: Extras.Key<Bitmap.Config>
    get() = bitmapConfigKey

private val bitmapConfigKey = Extras.Key(default = DEFAULT_BITMAP_CONFIG)

// endregion
// region colorSpace

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
    @RequiresApi(26) get() = getExtra(colorSpaceKey)

val Options.colorSpace: ColorSpace?
    @RequiresApi(26) get() = getExtra(colorSpaceKey)

val Extras.Key.Companion.colorSpace: Extras.Key<ColorSpace?>
    @RequiresApi(26) get() = colorSpaceKey

private val colorSpaceKey = Extras.Key(default = NULL_COLOR_SPACE)

// endregion
// region premultipliedAlpha

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
    get() = getExtra(premultipliedAlphaKey)

val Options.premultipliedAlpha: Boolean
    get() = getExtra(premultipliedAlphaKey)

val Extras.Key.Companion.premultipliedAlpha: Extras.Key<Boolean>
    get() = premultipliedAlphaKey

private val premultipliedAlphaKey = Extras.Key(default = true)

// endregion
// region lifecycle

/**
 * Set the [Lifecycle] for this request.
 *
 * Requests are queued while the lifecycle is not at least [Lifecycle.State.STARTED].
 * Requests are cancelled when the lifecycle reaches [Lifecycle.State.DESTROYED].
 *
 * If this is null or is not set the [ImageLoader] will attempt to find the lifecycle
 * for this request through [ImageRequest.context].
 */
fun ImageRequest.Builder.lifecycle(owner: LifecycleOwner?) = lifecycle(owner?.lifecycle)

fun ImageRequest.Builder.lifecycle(lifecycle: Lifecycle?) = apply {
    extras[lifecycleKey] = lifecycle
}

val ImageRequest.lifecycle: Lifecycle?
    get() = getExtra(lifecycleKey)

val Options.lifecycle: Lifecycle?
    get() = getExtra(lifecycleKey)

val Extras.Key.Companion.lifecycle: Extras.Key<Lifecycle?>
    get() = lifecycleKey

private val lifecycleKey = Extras.Key<Lifecycle?>(default = null)

// endregion
// region allowConversionToBitmap

/**
 * Allow converting the result drawable to a bitmap to apply any [transformations].
 *
 * If false and the result drawable is not a [BitmapDrawable] any [transformations] will
 * be ignored.
 */
fun ImageRequest.Builder.allowConversionToBitmap(enable: Boolean) = apply {
    extras[allowConversionToBitmapKey] = enable
}

fun ImageLoader.Builder.allowConversionToBitmap(enable: Boolean) = apply {
    extras[allowConversionToBitmapKey] = enable
}

val ImageRequest.allowConversionToBitmap: Boolean
    get() = getExtra(allowConversionToBitmapKey)

val Options.allowConversionToBitmap: Boolean
    get() = getExtra(allowConversionToBitmapKey)

val Extras.Key.Companion.allowConversionToBitmap: Extras.Key<Boolean>
    get() = allowConversionToBitmapKey

private val allowConversionToBitmapKey = Extras.Key(default = true)

// endregion
// region allowHardware

/**
 * Allow the use of [Bitmap.Config.HARDWARE].
 *
 * If false, any use of [Bitmap.Config.HARDWARE] will be treated as
 * [Bitmap.Config.ARGB_8888].
 *
 * NOTE: Setting this to false this will reduce performance on API 26 and above. Only
 * disable this if necessary.
 */
fun ImageRequest.Builder.allowHardware(enable: Boolean) = apply {
    extras[allowHardwareKey] = enable
}

fun ImageLoader.Builder.allowHardware(enable: Boolean) = apply {
    extras[allowHardwareKey] = enable
}

val ImageRequest.allowHardware: Boolean
    get() = getExtra(allowHardwareKey)

val Options.allowHardware: Boolean
    get() = getExtra(allowHardwareKey)

val Extras.Key.Companion.allowHardware: Extras.Key<Boolean>
    get() = allowHardwareKey

private val allowHardwareKey = Extras.Key(default = true)

// endregion
// region allowRgb565

/**
 * Allow automatically using [Bitmap.Config.RGB_565] when an image is guaranteed to not
 * have alpha.
 *
 * This will reduce the visual quality of the image, but will also reduce memory usage.
 *
 * Prefer only enabling this for low memory and resource constrained devices.
 */
fun ImageRequest.Builder.allowRgb565(enable: Boolean) = apply {
    extras[allowRgb565Key] = enable
}

fun ImageLoader.Builder.allowRgb565(enable: Boolean) = apply {
    extras[allowRgb565Key] = enable
}

val ImageRequest.allowRgb565: Boolean
    get() = getExtra(allowRgb565Key)

val Options.allowRgb565: Boolean
    get() = getExtra(allowRgb565Key)

val Extras.Key.Companion.allowRgb565: Extras.Key<Boolean>
    get() = allowRgb565Key

private val allowRgb565Key = Extras.Key(default = false)

// endregion
