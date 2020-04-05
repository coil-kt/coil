@file:JvmName("ImageLoaders")
@file:Suppress("NOTHING_TO_INLINE", "unused")

package coil.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.request.GetRequest
import coil.request.GetRequestBuilder
import coil.request.LoadRequest
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
    replaceWith = ReplaceWith(
        expression = "this.execute(LoadRequest.Builder(context).data(uri).apply(builder).build())",
        imports = ["coil.request.LoadRequest"]
    )
)
inline fun ImageLoader.load(
    context: Context,
    uri: String?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = execute(LoadRequest.Builder(context).data(uri).apply(builder).build())

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.get(GetRequest.Builder().data(uri).apply(builder).build())",
        imports = ["coil.request.GetRequest"]
    )
)
suspend inline fun ImageLoader.get(
    uri: String,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequest.Builder().data(uri).apply(builder).build())

// endregion
// region URL (HttpUrl)

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.execute(LoadRequest.Builder(context).data(url).apply(builder).build())",
        imports = ["coil.request.LoadRequest"]
    )
)
inline fun ImageLoader.load(
    context: Context,
    url: HttpUrl?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = execute(LoadRequest.Builder(context).data(url).apply(builder).build())

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.get(GetRequest.Builder().data(url).apply(builder).build())",
        imports = ["coil.request.GetRequest"]
    )
)
suspend inline fun ImageLoader.get(
    url: HttpUrl,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequest.Builder().data(url).apply(builder).build())

// endregion
// region Uri

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.execute(LoadRequest.Builder(context).data(uri).apply(builder).build())",
        imports = ["coil.request.LoadRequest"]
    )
)
inline fun ImageLoader.load(
    context: Context,
    uri: Uri?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = execute(LoadRequest.Builder(context).data(uri).apply(builder).build())

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.get(GetRequest.Builder().data(uri).apply(builder).build())",
        imports = ["coil.request.GetRequest"]
    )
)
suspend inline fun ImageLoader.get(
    uri: Uri,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequest.Builder().data(uri).apply(builder).build())

// endregion
// region File

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.execute(LoadRequest.Builder(context).data(file).apply(builder).build())",
        imports = ["coil.request.LoadRequest"]
    )
)
inline fun ImageLoader.load(
    context: Context,
    file: File?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = execute(LoadRequest.Builder(context).data(file).apply(builder).build())

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.get(GetRequest.Builder().data(file).apply(builder).build())",
        imports = ["coil.request.GetRequest"]
    )
)
suspend inline fun ImageLoader.get(
    file: File,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequest.Builder().data(file).apply(builder).build())

// endregion
// region Resource

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.execute(LoadRequest.Builder(context).data(drawableRes).apply(builder).build())",
        imports = ["coil.request.LoadRequest"]
    )
)
inline fun ImageLoader.load(
    context: Context,
    @DrawableRes drawableRes: Int,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = execute(LoadRequest.Builder(context).data(drawableRes).apply(builder).build())

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.get(GetRequest.Builder().data(drawableRes).apply(builder).build())",
        imports = ["coil.request.GetRequest"]
    )
)
suspend inline fun ImageLoader.get(
    @DrawableRes drawableRes: Int,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequest.Builder().data(drawableRes).apply(builder).build())

// endregion
// region Drawable

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.execute(LoadRequest.Builder(context).data(drawable).apply(builder).build())",
        imports = ["coil.request.LoadRequest"]
    )
)
inline fun ImageLoader.load(
    context: Context,
    drawable: Drawable?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = execute(LoadRequest.Builder(context).data(drawable).apply(builder).build())

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.get(GetRequest.Builder().data(drawable).apply(builder).build())",
        imports = ["coil.request.GetRequest"]
    )
)
suspend inline fun ImageLoader.get(
    drawable: Drawable,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequest.Builder().data(drawable).apply(builder).build())

// endregion
// region Bitmap

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.execute(LoadRequest.Builder(context).data(bitmap).apply(builder).build())",
        imports = ["coil.request.LoadRequest"]
    )
)
inline fun ImageLoader.load(
    context: Context,
    bitmap: Bitmap?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = execute(LoadRequest.Builder(context).data(bitmap).apply(builder).build())

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.get(GetRequest.Builder().data(bitmap).apply(builder).build())",
        imports = ["coil.request.GetRequest"]
    )
)
suspend inline fun ImageLoader.get(
    bitmap: Bitmap,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequest.Builder().data(bitmap).apply(builder).build())

// endregion
// region Any

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.execute(LoadRequest.Builder(context).data(data).apply(builder).build())",
        imports = ["coil.request.LoadRequest"]
    )
)
inline fun ImageLoader.loadAny(
    context: Context,
    data: Any?,
    builder: LoadRequestBuilder.() -> Unit = {}
): RequestDisposable = execute(LoadRequest.Builder(context).data(data).apply(builder).build())

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "this.get(GetRequest.Builder().data(data).apply(builder).build())",
        imports = ["coil.request.GetRequest"]
    )
)
suspend inline fun ImageLoader.getAny(
    data: Any,
    builder: GetRequestBuilder.() -> Unit = {}
): Drawable = get(GetRequest.Builder().data(data).apply(builder).build())

// endregion
// region Request Creation

@Deprecated(
    message = "Replace with the LoadRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "LoadRequest.Builder(context)",
        imports = ["coil.request.LoadRequest"]
    )
)
inline fun ImageLoader.newLoadBuilder(context: Context) = LoadRequest.Builder(context)

@Deprecated(
    message = "Replace with the GetRequest.Builder API.",
    replaceWith = ReplaceWith(
        expression = "GetRequest.Builder()",
        imports = ["coil.request.GetRequest"]
    )
)
inline fun ImageLoader.newGetBuilder() = GetRequest.Builder()

// endregion
