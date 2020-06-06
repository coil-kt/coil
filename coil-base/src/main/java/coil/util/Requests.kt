@file:JvmName("-Requests")
@file:Suppress("NOTHING_TO_INLINE")

package coil.util

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import coil.fetch.Fetcher
import coil.request.GlobalLifecycle
import coil.request.Request
import coil.target.ViewTarget

/** Used to resolve [Request.placeholder], [Request.error], and [Request.fallback]. */
internal fun Request.getDrawableCompat(drawable: Drawable?, @DrawableRes resId: Int): Drawable? {
    return drawable.takeIf { it !== EMPTY_DRAWABLE } ?: if (resId != 0) context.getDrawableCompat(resId) else null
}

internal fun Request.getLifecycle(): Lifecycle {
    return when {
        lifecycle != null -> lifecycle
        target is ViewTarget<*> -> target.view.context.getLifecycle()
        else -> context.getLifecycle()
    } ?: GlobalLifecycle
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
