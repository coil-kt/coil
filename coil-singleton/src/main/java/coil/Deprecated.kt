@file:JvmName("ImageViews")
@file:Suppress("DEPRECATION", "PackageDirectoryMismatch", "unused")

package coil.api

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.annotation.DrawableRes
import coil.Coil
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import okhttp3.HttpUrl
import java.io.File
import coil.clear as _clear
import coil.load as _load
import coil.loadAny as _loadAny

@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    uri: String?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable = _load(uri, imageLoader, builder)

@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    url: HttpUrl?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable = _load(url, imageLoader, builder)

@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    uri: Uri?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable = _load(uri, imageLoader, builder)

@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    file: File?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable = _load(file, imageLoader, builder)

@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    @DrawableRes drawableResId: Int,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable = _load(drawableResId, imageLoader, builder)

@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    drawable: Drawable?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable = _load(drawable, imageLoader, builder)

@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.load(
    bitmap: Bitmap?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable = _load(bitmap, imageLoader, builder)

@Deprecated("Replace `coil.api.load` with `coil.load`.")
@JvmSynthetic
inline fun ImageView.loadAny(
    data: Any?,
    imageLoader: ImageLoader = Coil.imageLoader(context),
    builder: ImageRequest.Builder.() -> Unit = {}
): Disposable = _loadAny(data, imageLoader, builder)

@Deprecated("Replace `coil.api.clear` with `coil.clear`.")
fun ImageView.clear() = _clear()
