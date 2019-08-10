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
//     memoryCachePolicy(CachePolicy.DISABLED)
//     size(1080, 1920)
// }
// ```

// region URL (String)

inline fun Coil.load(
    context: Context,
    url: String?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = loader().load(context, url, builder)

suspend inline fun Coil.get(
    url: String,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().get(url, builder)

// endregion
// region URL (HttpUrl)

inline fun Coil.load(
    context: Context,
    url: HttpUrl?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = loader().load(context, url, builder)

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
): RequestDisposable = loader().load(context, uri, builder)

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
): RequestDisposable = loader().load(context, file, builder)

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
): RequestDisposable = loader().load(context, drawableRes, builder)

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
): RequestDisposable = loader().load(context, drawable, builder)

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
): RequestDisposable = loader().load(context, bitmap, builder)

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
): RequestDisposable = loader().loadAny(context, data, builder)

suspend inline fun Coil.getAny(
    data: Any,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = loader().getAny(data, builder)

// endregion
