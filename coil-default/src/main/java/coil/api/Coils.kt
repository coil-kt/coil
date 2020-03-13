@file:JvmName("Coils")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DrawableRes
import coil.Coil
import coil.request.GetRequestBuilder
import coil.request.LoadRequestBuilder
import coil.request.RequestDisposable
import okhttp3.HttpUrl
import java.io.File

// This file defines a collection of type-safe load and get extension functions for Coil.
//
// Example:
// ```
// Coil.load(context, "https://www.example.com/image.jpg") {
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
inline fun Coil.load(
    context: Context,
    uri: String?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, uri, builder)

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(uri).apply(builder).launch()")
)
suspend inline fun Coil.get(
    uri: String,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(uri, builder)

// endregion
// region URL (HttpUrl)

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(url).apply(builder).launch()")
)
inline fun Coil.load(
    context: Context,
    url: HttpUrl?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, url, builder)

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(url).apply(builder).launch()")
)
suspend inline fun Coil.get(
    url: HttpUrl,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(url, builder)

// endregion
// region Uri

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(uri).apply(builder).launch()")
)
inline fun Coil.load(
    context: Context,
    uri: Uri?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, uri, builder)

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(uri).apply(builder).launch()")
)
suspend inline fun Coil.get(
    uri: Uri,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(uri, builder)

// endregion
// region File

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(file).apply(builder).launch()")
)
inline fun Coil.load(
    context: Context,
    file: File?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, file, builder)

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(file).apply(builder).launch()")
)
suspend inline fun Coil.get(
    file: File,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(file, builder)

// endregion
// region Resource

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(drawableRes).apply(builder).launch()")
)
inline fun Coil.load(
    context: Context,
    @DrawableRes drawableRes: Int,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, drawableRes, builder)

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(drawableRes).apply(builder).launch()")
)
suspend inline fun Coil.get(
    @DrawableRes drawableRes: Int,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(drawableRes, builder)

// endregion
// region Drawable

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(drawable).apply(builder).launch()")
)
inline fun Coil.load(
    context: Context,
    drawable: Drawable?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, drawable, builder)

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(drawable).apply(builder).launch()")
)
suspend inline fun Coil.get(
    drawable: Drawable,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(drawable, builder)

// endregion
// region Bitmap

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(bitmap).apply(builder).launch()")
)
inline fun Coil.load(
    context: Context,
    bitmap: Bitmap?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, bitmap, builder)

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(bitmap).apply(builder).launch()")
)
suspend inline fun Coil.get(
    bitmap: Bitmap,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(bitmap, builder)

// endregion
// region Any

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith("load(context).data(data).apply(builder).launch()")
)
inline fun Coil.loadAny(
    context: Context,
    data: Any?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).loadAny(context, data, builder)

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith("get().data(data).apply(builder).launch()")
)
suspend inline fun Coil.getAny(
    data: Any,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().getAny(data, builder)

// endregion
