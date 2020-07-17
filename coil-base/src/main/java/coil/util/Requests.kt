@file:JvmName("-Requests")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.fetch.Fetcher
import coil.request.ImageRequest
import coil.size.DisplaySizeResolver
import coil.size.Precision
import coil.size.ViewSizeResolver
import coil.target.ViewTarget

/** Used to resolve [ImageRequest.placeholder], [ImageRequest.error], and [ImageRequest.fallback]. */
internal fun ImageRequest.getDrawableCompat(
    drawable: Drawable?,
    @DrawableRes resId: Int?,
    default: Drawable?
): Drawable? {
    return when {
        drawable != null -> drawable
        resId != null -> if (resId == 0) null else context.getDrawableCompat(resId)
        else -> default
    }
}

/** Ensure [ImageRequest.fetcher] is valid for [data]. */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> ImageRequest.fetcher(data: T): Fetcher<T>? {
    val (fetcher, type) = fetcher ?: return null
    check(type.isAssignableFrom(data::class.java)) {
        "${fetcher.javaClass.name} cannot handle data with type ${data.javaClass.name}."
    }
    return fetcher as Fetcher<T>
}

/** Return true if the request does not require the output image's size to match the requested dimensions exactly. */
internal val ImageRequest.allowInexactSize: Boolean
    get() = when (precision) {
        Precision.EXACT -> false
        Precision.INEXACT -> true
        Precision.AUTOMATIC -> run {
            // If both our target and size resolver reference the same ImageView, allow the
            // dimensions to be inexact as the ImageView will scale the output image automatically.
            if (target is ViewTarget<*> && target.view is ImageView &&
                sizeResolver is ViewSizeResolver<*> && sizeResolver.view === target.view) {
                return true
            }

            // If we implicitly fall back to a DisplaySizeResolver, allow the dimensions to be inexact.
            if (defined.sizeResolver == null && sizeResolver is DisplaySizeResolver) return true

            // Else, require the dimensions to be exact.
            return false
        }
    }
