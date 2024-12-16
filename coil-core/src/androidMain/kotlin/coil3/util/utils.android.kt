package coil3.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_INSIDE
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import coil3.BitmapImage
import coil3.Image
import coil3.Uri
import coil3.pathSegments
import coil3.request.ImageRequest
import coil3.size.Scale
import coil3.transform.Transformation
import java.io.File

internal actual fun println(level: Logger.Level, tag: String, message: String) {
    Log.println(level.toInt(), tag, message)
}

private fun Logger.Level.toInt() = when (this) {
    Logger.Level.Verbose -> Log.VERBOSE
    Logger.Level.Debug -> Log.DEBUG
    Logger.Level.Info -> Log.INFO
    Logger.Level.Warn -> Log.WARN
    Logger.Level.Error -> Log.ERROR
}

/** Required for compatibility with API 25 and below. */
internal val NULL_COLOR_SPACE: ColorSpace? = null

internal val Configuration.nightMode: Int
    get() = uiMode and Configuration.UI_MODE_NIGHT_MASK

internal val Drawable.isVector: Boolean
    get() = this is VectorDrawable || this is VectorDrawableCompat

internal val Drawable.width: Int
    get() = (this as? BitmapDrawable)?.bitmap?.width ?: intrinsicWidth

internal val Drawable.height: Int
    get() = (this as? BitmapDrawable)?.bitmap?.height ?: intrinsicHeight

/** https://github.com/coil-kt/coil/issues/675 */
internal val Context.safeCacheDir: File
    get() {
        val cacheDir = checkNotNull(cacheDir) { "cacheDir == null" }
        return cacheDir.apply { mkdirs() }
    }

internal fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

/**
 * An allowlist of valid bitmap configs for the input and output bitmaps of
 * [Transformation.transform].
 */
internal val VALID_TRANSFORMATION_CONFIGS = if (SDK_INT >= 26) {
    arrayOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGBA_F16)
} else {
    arrayOf(Bitmap.Config.ARGB_8888)
}

/**
 * Prefer hardware bitmaps on API 26 and above since they are optimized for drawing without
 * transformations.
 */
internal val DEFAULT_BITMAP_CONFIG = if (SDK_INT >= 26) {
    Bitmap.Config.HARDWARE
} else {
    Bitmap.Config.ARGB_8888
}

internal fun ImageRequest.getDrawableCompat(
    drawable: Drawable?,
    @DrawableRes resId: Int?,
    default: Drawable?,
): Drawable? = when {
    drawable != null -> drawable
    resId != null -> if (resId != 0) {
        context.getDrawableCompat(resId)
    } else {
        null
    }
    else -> default
}

internal const val ASSET_FILE_PATH_ROOT = "android_asset"

internal actual fun isAssetUri(uri: Uri): Boolean {
    return uri.scheme == SCHEME_FILE && uri.pathSegments.firstOrNull() == ASSET_FILE_PATH_ROOT
}

internal val ImageView.scale: Scale
    get() = when (scaleType) {
        FIT_START, FIT_CENTER, FIT_END, CENTER_INSIDE -> Scale.FIT
        else -> Scale.FILL
    }

internal actual fun Image.prepareToDraw() {
    if (this is BitmapImage) {
        bitmap.prepareToDraw()
    }
}
