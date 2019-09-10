@file:JvmName("ImageViews")
@file:Suppress("unused")

package coil.api

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.Coil
import coil.ImageLoader
import coil.request.LoadRequestBuilder
import coil.request.RequestDisposable
import okhttp3.HttpUrl
import java.io.File

// This file defines a collection of type-safe load and get extension functions for ImageView.
//
// Example:
// ```
// imageView.load("https://www.example.com/image.jpg") {
//     networkCachePolicy(CachePolicy.DISABLED)
//     transformations(CircleCropTransformation())
// }
// ```

// region URL (String)

inline fun ImageView.load(
    uri: String?,
    imageLoader: ImageLoader = Coil.loader(),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context, uri) {
        target(this@load)
        builder()
    }
}

// endregion
// region URL (HttpUrl)

inline fun ImageView.load(
    url: HttpUrl?,
    imageLoader: ImageLoader = Coil.loader(),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context, url) {
        target(this@load)
        builder()
    }
}

// endregion
// region Uri

inline fun ImageView.load(
    uri: Uri?,
    imageLoader: ImageLoader = Coil.loader(),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context, uri) {
        target(this@load)
        builder()
    }
}

// endregion
// region File

inline fun ImageView.load(
    file: File?,
    imageLoader: ImageLoader = Coil.loader(),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context, file) {
        target(this@load)
        builder()
    }
}

// endregion
// region Resource

inline fun ImageView.load(
    @DrawableRes drawableRes: Int,
    imageLoader: ImageLoader = Coil.loader(),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context, drawableRes) {
        target(this@load)
        builder()
    }
}

// endregion
// region Drawable

inline fun ImageView.load(
    drawable: Drawable?,
    imageLoader: ImageLoader = Coil.loader(),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context, drawable) {
        target(this@load)
        builder()
    }
}

// endregion
// region Bitmap

inline fun ImageView.load(
    bitmap: Bitmap?,
    imageLoader: ImageLoader = Coil.loader(),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.load(context, bitmap) {
        target(this@load)
        builder()
    }
}

// endregion
// region Any

inline fun ImageView.loadAny(
    data: Any?,
    imageLoader: ImageLoader = Coil.loader(),
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable {
    return imageLoader.loadAny(context, data) {
        target(this@loadAny)
        builder()
    }
}

// endregion
