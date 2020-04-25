@file:JvmName("-Requests")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import coil.DefaultRequestOptions
import coil.fetch.Fetcher
import coil.request.LoadRequest
import coil.request.Request

/** Used to resolve [LoadRequest.placeholder], [Request.error], and [Request.fallback]. */
internal fun Request.getDrawableCompat(drawable: Drawable?, @DrawableRes resId: Int): Drawable? {
    return drawable.takeIf { it !== EMPTY_DRAWABLE } ?: if (resId != 0) context.getDrawableCompat(resId) else null
}

internal inline fun Request.placeholderOrDefault(defaults: DefaultRequestOptions): Drawable? {
    return if (this is LoadRequest && placeholderDrawable != null) placeholder else defaults.placeholder
}

internal inline fun Request.errorOrDefault(defaults: DefaultRequestOptions): Drawable? {
    return if (this is LoadRequest && errorDrawable != null) error else defaults.error
}

internal inline fun Request.fallbackOrDefault(defaults: DefaultRequestOptions): Drawable? {
    return if (this is LoadRequest && fallbackDrawable != null) fallback else defaults.fallback
}

internal inline fun Request.bitmapConfigOrDefault(defaults: DefaultRequestOptions): Bitmap.Config {
    return bitmapConfig ?: defaults.bitmapConfig
}

/** Ensure [Request.fetcher] is valid for [data]. */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Request.validateFetcher(data: T): Fetcher<T>? {
    val (type, fetcher) = fetcher ?: return null

    check(type.isAssignableFrom(data::class.java)) {
        "${fetcher.javaClass.name} cannot handle data with type ${data.javaClass.name}."
    }

    return fetcher as Fetcher<T>
}
