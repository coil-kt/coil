@file:JvmName("Coils")
@file:Suppress("unused")

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

inline fun Coil.load(
    context: Context,
    uri: String?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, uri, builder)

suspend inline fun Coil.get(
    context: Context,
    uri: String,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = imageLoader(context).get(uri, builder)

@Deprecated(
    message = "Migrate to Coil.get(context, uri).",
    replaceWith = ReplaceWith("get(context, uri, builder)")
)
suspend inline fun Coil.get(
    uri: String,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(uri, builder)

// endregion
// region URL (HttpUrl)

inline fun Coil.load(
    context: Context,
    url: HttpUrl?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, url, builder)

suspend inline fun Coil.get(
    context: Context,
    url: HttpUrl,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = imageLoader(context).get(url, builder)

@Deprecated(
    message = "Migrate to Coil.get(context, url).",
    replaceWith = ReplaceWith("get(context, url, builder)")
)
suspend inline fun Coil.get(
    url: HttpUrl,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(url, builder)

// endregion
// region Uri

inline fun Coil.load(
    context: Context,
    uri: Uri?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, uri, builder)

suspend inline fun Coil.get(
    context: Context,
    uri: Uri,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = imageLoader(context).get(uri, builder)

@Deprecated(
    message = "Migrate to Coil.get(context, uri).",
    replaceWith = ReplaceWith("get(context, uri, builder)")
)
suspend inline fun Coil.get(
    uri: Uri,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(uri, builder)

// endregion
// region File

inline fun Coil.load(
    context: Context,
    file: File?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, file, builder)

suspend inline fun Coil.get(
    context: Context,
    file: File,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = imageLoader(context).get(file, builder)

@Deprecated(
    message = "Migrate to Coil.get(context, file).",
    replaceWith = ReplaceWith("get(context, file, builder)")
)
suspend inline fun Coil.get(
    file: File,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(file, builder)

// endregion
// region Resource

inline fun Coil.load(
    context: Context,
    @DrawableRes drawableRes: Int,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, drawableRes, builder)

suspend inline fun Coil.get(
    context: Context,
    @DrawableRes drawableRes: Int,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = imageLoader(context).get(drawableRes, builder)

@Deprecated(
    message = "Migrate to Coil.get(context, drawableRes).",
    replaceWith = ReplaceWith("get(context, drawableRes, builder)")
)
suspend inline fun Coil.get(
    @DrawableRes drawableRes: Int,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(drawableRes, builder)

// endregion
// region Drawable

inline fun Coil.load(
    context: Context,
    drawable: Drawable?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, drawable, builder)

suspend inline fun Coil.get(
    context: Context,
    drawable: Drawable,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = imageLoader(context).get(drawable, builder)

@Deprecated(
    message = "Migrate to Coil.get(context, drawable).",
    replaceWith = ReplaceWith("get(context, drawable, builder)")
)
suspend inline fun Coil.get(
    drawable: Drawable,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(drawable, builder)

// endregion
// region Bitmap

inline fun Coil.load(
    context: Context,
    bitmap: Bitmap?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).load(context, bitmap, builder)

suspend inline fun Coil.get(
    context: Context,
    bitmap: Bitmap,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = imageLoader(context).get(bitmap, builder)

@Deprecated(
    message = "Migrate to Coil.get(context, bitmap).",
    replaceWith = ReplaceWith("get(context, bitmap, builder)")
)
suspend inline fun Coil.get(
    bitmap: Bitmap,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(bitmap, builder)

// endregion
// region Any

inline fun Coil.loadAny(
    context: Context,
    data: Any?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = imageLoader(context).loadAny(context, data, builder)

suspend inline fun Coil.getAny(
    context: Context,
    data: Any,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = imageLoader(context).getAny(data, builder)

@Deprecated(
    message = "Migrate to Coil.getAny(context, data).",
    replaceWith = ReplaceWith("getAny(context, data, builder)")
)
suspend inline fun Coil.getAny(
    data: Any,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().getAny(data, builder)

// endregion
