@file:JvmName("ImageLoaders")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.request.GetRequestBuilder
import coil.request.LoadRequestBuilder
import coil.request.RequestDisposable
import okhttp3.HttpUrl
import java.io.File

// This file defines a collection of type-safe load and get extension functions for ImageLoaders.
//
// Example:
// ```
// imageLoader.load(context, "https://www.example.com/image.jpg") {
//     networkCachePolicy(CachePolicy.DISABLED)
//     transformations(CircleCropTransformation())
//     target(imageView)
// }
// ```

// region URL (String)

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(uri).apply(builder).launch()")
)
inline fun ImageLoader.load(
    context: Context,
    uri: String?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(context).data(uri).apply(builder).launch()

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(uri).apply(builder).launch()")
)
suspend inline fun ImageLoader.get(
    uri: String,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get().data(uri).apply(builder).launch()

// endregion
// region URL (HttpUrl)

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(url).apply(builder).launch()")
)
inline fun ImageLoader.load(
    context: Context,
    url: HttpUrl?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(context).data(url).apply(builder).launch()

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(url).apply(builder).launch()")
)
suspend inline fun ImageLoader.get(
    url: HttpUrl,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get().data(url).apply(builder).launch()

// endregion
// region Uri

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(uri).apply(builder).launch()")
)
inline fun ImageLoader.load(
    context: Context,
    uri: Uri?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(context).data(uri).apply(builder).launch()

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(uri).apply(builder).launch()")
)
suspend inline fun ImageLoader.get(
    uri: Uri,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get().data(uri).apply(builder).launch()

// endregion
// region File

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(file).apply(builder).launch()")
)
inline fun ImageLoader.load(
    context: Context,
    file: File?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(context).data(file).apply(builder).launch()

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(file).apply(builder).launch()")
)
suspend inline fun ImageLoader.get(
    file: File,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get().data(file).apply(builder).launch()

// endregion
// region Resource

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(drawableRes).apply(builder).launch()")
)
inline fun ImageLoader.load(
    context: Context,
    @DrawableRes drawableRes: Int,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(context).data(drawableRes).apply(builder).launch()

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(drawableRes).apply(builder).launch()")
)
suspend inline fun ImageLoader.get(
    @DrawableRes drawableRes: Int,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get().data(drawableRes).apply(builder).launch()

// endregion
// region Drawable

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(drawable).apply(builder).launch()")
)
inline fun ImageLoader.load(
    context: Context,
    drawable: Drawable?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(context).data(drawable).apply(builder).launch()

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(drawable).apply(builder).launch()")
)
suspend inline fun ImageLoader.get(
    drawable: Drawable,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get().data(drawable).apply(builder).launch()

// endregion
// region Bitmap

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(bitmap).apply(builder).launch()")
)
inline fun ImageLoader.load(
    context: Context,
    bitmap: Bitmap?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(context).data(bitmap).apply(builder).launch()

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(bitmap).apply(builder).launch()")
)
suspend inline fun ImageLoader.get(
    bitmap: Bitmap,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get().data(bitmap).apply(builder).launch()

// endregion
// region Any

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(data).apply(builder).launch()")
)
inline fun ImageLoader.loadAny(
    context: Context,
    data: Any?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = load(context).data(data).apply(builder).launch()

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(data).apply(builder).launch()")
)
suspend inline fun ImageLoader.getAny(
    data: Any,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get().data(data).apply(builder).launch()

// endregion
// region Request Creation

@Deprecated(
    message = "Replace with class member function.",
    replaceWith = ReplaceWith("load(context)")
)
inline fun ImageLoader.newLoadBuilder(context: Context) = load(context)

@Deprecated(
    message = "Replace with class member function.",
    replaceWith = ReplaceWith("get()")
)
inline fun ImageLoader.newGetBuilder() = get()

// endregion
