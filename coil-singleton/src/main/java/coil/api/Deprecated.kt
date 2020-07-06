@file:JvmName("ImageViews")
@file:Suppress("DEPRECATION", "unused")

package coil.api

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.Coil
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.RequestDisposable
import coil.util.CoilUtils
import okhttp3.HttpUrl
import java.io.File

/** @see ImageView.loadAny */
@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    uri: String?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): RequestDisposable = loadAny(uri, imageLoader, builder)

/** @see ImageView.loadAny */
@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    url: HttpUrl?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): RequestDisposable = loadAny(url, imageLoader, builder)

/** @see ImageView.loadAny */
@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    uri: Uri?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): RequestDisposable = loadAny(uri, imageLoader, builder)

/** @see ImageView.loadAny */
@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    file: File?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): RequestDisposable = loadAny(file, imageLoader, builder)

/** @see ImageView.loadAny */
@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    @DrawableRes drawableResId: Int,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): RequestDisposable = loadAny(drawableResId, imageLoader, builder)

/** @see ImageView.loadAny */
@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    drawable: Drawable?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): RequestDisposable = loadAny(drawable, imageLoader, builder)

/** @see ImageView.loadAny */
@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    bitmap: Bitmap?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): RequestDisposable = loadAny(bitmap, imageLoader, builder)

/**
 * Load the image referenced by [data] and set it on this [ImageView].
 *
 * This is the type-unsafe version of [ImageView.load].
 *
 * Example:
 * ```
 * imageView.load("https://www.example.com/image.jpg") {
 *     crossfade(true)
 *     transformations(CircleCropTransformation())
 * }
 * ```
 *
 * @param data The data to load.
 * @param imageLoader The [ImageLoader] that will be used to enqueue the [ImageRequest].
 * @param builder An optional lambda to configure the request before it is enqueued.
 */
@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.loadAny(
    data: Any?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): RequestDisposable {
    val request = ImageRequest.Builder(context)
        .data(data)
        .target(this)
        .apply(builder)
        .build()
    return imageLoader.enqueue(request)
}

/**
 * Cancel any in progress requests and clear all resources associated with this [ImageView].
 */
@Deprecated("Replace `coil.api.clear` with `coil.clear`.")
fun ImageView.clear() {
    CoilUtils.clear(this)
}
