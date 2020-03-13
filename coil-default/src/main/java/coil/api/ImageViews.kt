@file:JvmName("ImageViews")
@file:Suppress("unused")
@file:OptIn(ExperimentalCoilApi::class)

package coil.api

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.request.LoadRequestBuilder
import coil.request.RequestDisposable
import coil.util.CoilUtils
import okhttp3.HttpUrl
import java.io.File

// This file defines a collection of type-safe load extension functions for ImageViews.
//
// Example:
// ```
// imageView.load("https://www.example.com/image.jpg") {
//     networkCachePolicy(CachePolicy.DISABLED)
//     transformations(CircleCropTransformation())
// }
// ```

// region URL (String)

@JvmSynthetic
inline fun ImageView.load(
    uri: String?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context)
        .data(uri)
        .target(this)
        .apply(builder)
        .launch()
}

// endregion
// region URL (HttpUrl)

@JvmSynthetic
inline fun ImageView.load(
    url: HttpUrl?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context)
        .data(url)
        .target(this)
        .apply(builder)
        .launch()
}

// endregion
// region Uri

@JvmSynthetic
inline fun ImageView.load(
    uri: Uri?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context)
        .data(uri)
        .target(this)
        .apply(builder)
        .launch()
}

// endregion
// region File

@JvmSynthetic
inline fun ImageView.load(
    file: File?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context)
        .data(file)
        .target(this)
        .apply(builder)
        .launch()
}

// endregion
// region Resource

@JvmSynthetic
inline fun ImageView.load(
    @DrawableRes drawableRes: Int,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context)
        .data(drawableRes)
        .target(this)
        .apply(builder)
        .launch()
}

// endregion
// region Drawable

@JvmSynthetic
inline fun ImageView.load(
    drawable: Drawable?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context)
        .data(drawable)
        .target(this)
        .apply(builder)
        .launch()
}

// endregion
// region Bitmap

@JvmSynthetic
inline fun ImageView.load(
    bitmap: Bitmap?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context)
        .data(bitmap)
        .target(this)
        .apply(builder)
        .launch()
}

// endregion
// region Any

@JvmSynthetic
inline fun ImageView.loadAny(
    data: Any?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context)
        .data(data)
        .target(this)
        .apply(builder)
        .launch()
}

// endregion
// region Other

/**
 * Cancel any in progress requests and clear any resources associated with this [ImageView].
 */
fun ImageView.clear() {
    CoilUtils.clear(this)
}

// endregion
