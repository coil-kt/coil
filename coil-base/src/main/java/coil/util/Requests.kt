@file:JvmName("-Requests")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import coil.fetch.Fetcher
import coil.request.ImageRequest

/** Used to resolve [ImageRequest.placeholder], [ImageRequest.error], and [ImageRequest.fallback]. */
internal fun ImageRequest.getDrawableCompat(
    drawable: Drawable?,
    @DrawableRes resId: Int?,
    default: Drawable?
): Drawable? {
    if (drawable != null) {
        return drawable
    }

    if (resId != null) {
        return if (resId == 0) null else context.getDrawableCompat(resId)
    }

    return default
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
