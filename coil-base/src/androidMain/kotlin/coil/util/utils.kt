package coil.util

import android.content.ContentResolver.SCHEME_FILE
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER_INSIDE
import android.widget.ImageView.ScaleType.FIT_CENTER
import android.widget.ImageView.ScaleType.FIT_END
import android.widget.ImageView.ScaleType.FIT_START
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import coil.request.ImageRequest
import coil.size.DisplaySizeResolver
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.target.ViewTarget
import coil.transform.Transformation
import java.io.File

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

/**
 * Used to resolve [ImageRequest.placeholder], [ImageRequest.error], and [ImageRequest.fallback].
 */
internal fun ImageRequest.getDrawableCompat(
    drawable: Drawable?,
    @DrawableRes resId: Int?,
    default: Drawable?
): Drawable? = when {
    drawable != null -> drawable
    resId != null -> if (resId == 0) null else context.getDrawableCompat(resId)
    else -> default
}

internal actual val ImageRequest.allowInexactSize: Boolean
    get() = when (precision) {
        Precision.EXACT -> false
        Precision.INEXACT -> true
        Precision.AUTOMATIC -> run {
            // If we haven't explicitly set a size and fell back to the default size resolver,
            // always allow inexact size.
            if (defined.sizeResolver == null && sizeResolver is DisplaySizeResolver) {
                return@run true
            }

            // If both our target and size resolver reference the same ImageView, allow the
            // dimensions to be inexact as the ImageView will scale the output image
            // automatically. Else, require the dimensions to be exact.
            return@run target is ViewTarget<*> &&
                sizeResolver is ViewSizeResolver<*> &&
                target.view is ImageView &&
                target.view === sizeResolver.view
        }
    }

internal val Uri.firstPathSegment: String?
    get() = pathSegments.firstOrNull()

internal const val ASSET_FILE_PATH_ROOT = "android_asset"

internal fun isAssetUri(uri: Uri): Boolean {
    return uri.scheme == SCHEME_FILE && uri.firstPathSegment == ASSET_FILE_PATH_ROOT
}

internal val ImageView.scale: Scale
    get() = when (scaleType) {
        FIT_START, FIT_CENTER, FIT_END, CENTER_INSIDE -> Scale.FIT
        else -> Scale.FILL
    }
