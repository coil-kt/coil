@file:JvmName("-Requests")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import coil.fetch.Fetcher
import coil.request.GlobalLifecycle
import coil.request.ImageRequest
import coil.target.ViewTarget

/** Used to resolve [ImageRequest.placeholder], [ImageRequest.error], and [ImageRequest.fallback]. */
internal fun ImageRequest.getDrawableCompat(drawable: Drawable?, @DrawableRes resId: Int): Drawable? {
    return drawable.takeIf { it !== EMPTY_DRAWABLE } ?: if (resId != 0) context.getDrawableCompat(resId) else null
}

internal fun ImageRequest.getLifecycle(): Lifecycle {
    return when {
        lifecycle != null -> lifecycle
        target is ViewTarget<*> -> target.view.context.getLifecycle()
        else -> context.getLifecycle()
    } ?: GlobalLifecycle
}

/** Ensure [ImageRequest.fetcher] is valid for [data]. */
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> ImageRequest.validateFetcher(data: T): Fetcher<T>? {
    val (type, fetcher) = fetcher ?: return null
    check(type.isAssignableFrom(data::class.java)) {
        "${fetcher.javaClass.name} cannot handle data with type ${data.javaClass.name}."
    }
    return fetcher as Fetcher<T>
}
