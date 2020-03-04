@file:Suppress("FunctionName", "NOTHING_TO_INLINE", "unused")
@file:OptIn(ExperimentalCoilApi::class)

package coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import coil.DefaultRequestOptions
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.Decoder
import coil.size.Precision
import coil.size.Scale
import coil.size.SizeResolver
import coil.target.Target
import coil.transform.Transformation
import coil.transition.Transition
import coil.util.getDrawableCompat
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.Headers

/**
 * A value object that represents an image request.
 *
 * @see LoadRequest
 * @see GetRequest
 */
sealed class Request {

    abstract val data: Any?

    abstract val target: Target?
    abstract val lifecycle: Lifecycle?
    abstract val transition: Transition?

    abstract val key: String?
    abstract val aliasKeys: List<String>
    abstract val listener: Listener?
    abstract val sizeResolver: SizeResolver?
    abstract val scale: Scale?
    abstract val precision: Precision
    abstract val decoder: Decoder?
    abstract val dispatcher: CoroutineDispatcher
    abstract val transformations: List<Transformation>
    abstract val bitmapConfig: Bitmap.Config
    abstract val colorSpace: ColorSpace?
    abstract val headers: Headers
    abstract val parameters: Parameters

    abstract val networkCachePolicy: CachePolicy
    abstract val diskCachePolicy: CachePolicy
    abstract val memoryCachePolicy: CachePolicy

    abstract val allowHardware: Boolean
    abstract val allowRgb565: Boolean

    abstract val placeholder: Drawable?
    abstract val error: Drawable?
    abstract val fallback: Drawable?

    /**
     * A set of callbacks for a [Request].
     */
    interface Listener {

        /**
         * Called when the request is dispatched and starts loading the image.
         */
        @MainThread
        fun onStart(data: Any) {}

        /**
         * Called when the request successfully loads the image.
         */
        @MainThread
        fun onSuccess(data: Any, source: DataSource) {}

        /**
         * Called when the request is cancelled.
         */
        @MainThread
        fun onCancel(data: Any?) {}

        /**
         * Called when the request fails to load the image.
         */
        @MainThread
        fun onError(data: Any?, throwable: Throwable) {}
    }
}

/**
 * A value object that represents a *load* image request.
 *
 * Instances can be created and executed ad hoc:
 * ```
 * imageLoader.load(context, "https://www.example.com/image.jpg") {
 *     crossfade(true)
 *     target(imageView)
 * }
 * ```
 *
 * Or instances can be created separately from the call that executes them:
 * ```
 * val request = LoadRequest.Builder(context, imageLoader.defaults)
 *     .data("https://www.example.com/image.jpg")
 *     .crossfade(true)
 *     .target(imageView)
 *     .build()
 * imageLoader.load(request)
 * ```
 *
 * @see LoadRequestBuilder
 * @see ImageLoader.load
 */
class LoadRequest internal constructor(
    val context: Context,
    override val data: Any?,
    override val target: Target?,
    override val lifecycle: Lifecycle?,
    override val transition: Transition?,
    override val key: String?,
    override val aliasKeys: List<String>,
    override val listener: Listener?,
    override val sizeResolver: SizeResolver?,
    override val scale: Scale?,
    override val precision: Precision,
    override val decoder: Decoder?,
    override val dispatcher: CoroutineDispatcher,
    override val transformations: List<Transformation>,
    override val bitmapConfig: Bitmap.Config,
    override val colorSpace: ColorSpace?,
    override val headers: Headers,
    override val parameters: Parameters,
    override val networkCachePolicy: CachePolicy,
    override val diskCachePolicy: CachePolicy,
    override val memoryCachePolicy: CachePolicy,
    override val allowHardware: Boolean,
    override val allowRgb565: Boolean,
    @DrawableRes internal val placeholderResId: Int,
    @DrawableRes internal val errorResId: Int,
    @DrawableRes internal val fallbackResId: Int,
    internal val placeholderDrawable: Drawable?,
    internal val errorDrawable: Drawable?,
    internal val fallbackDrawable: Drawable?
) : Request() {

    companion object {
        @JvmStatic
        @JvmName("builder")
        inline fun Builder(context: Context, defaults: DefaultRequestOptions) = LoadRequestBuilder(context, defaults)

        @JvmStatic
        @JvmName("builder")
        inline fun Builder(context: Context, request: LoadRequest) = LoadRequestBuilder(context, request)

        /** Create a new [LoadRequest] instance. */
        @Deprecated(
            message = "Use LoadRequest.Builder to create new instances.",
            replaceWith = ReplaceWith("LoadRequest.Builder(context, defaults).apply(builder).build()")
        )
        inline operator fun invoke(
            context: Context,
            defaults: DefaultRequestOptions,
            builder: LoadRequestBuilder.() -> Unit = {}
        ): LoadRequest = LoadRequestBuilder(context, defaults).apply(builder).build()

        /** Create a new [LoadRequest] instance. */
        @Deprecated(
            message = "Use LoadRequest.Builder to create new instances.",
            replaceWith = ReplaceWith("LoadRequest.Builder(context, request).apply(builder).build()")
        )
        inline operator fun invoke(
            context: Context,
            request: LoadRequest,
            builder: LoadRequestBuilder.() -> Unit = {}
        ): LoadRequest = LoadRequestBuilder(context, request).apply(builder).build()
    }

    override val placeholder: Drawable?
        get() = context.getDrawable(placeholderDrawable, placeholderResId)

    override val error: Drawable?
        get() = context.getDrawable(errorDrawable, errorResId)

    override val fallback: Drawable?
        get() = context.getDrawable(fallbackDrawable, fallbackResId)

    private fun Context.getDrawable(drawable: Drawable?, @DrawableRes resId: Int): Drawable? {
        return drawable ?: if (resId != 0) getDrawableCompat(resId) else null
    }

    /** Create a new [LoadRequestBuilder] instance using this as a base. */
    @JvmOverloads
    fun newBuilder(context: Context = this.context) = LoadRequestBuilder(context, this)
}

/**
 * A value object that represents a *get* image request.
 *
 * Instances can be created and executed ad hoc:
 * ```
 * val drawable = imageLoader.get("https://www.example.com/image.jpg") {
 *     size(1080, 1920)
 * }
 * ```
 *
 * Or instances can be created separately from the call that executes them:
 * ```
 * val request = GetRequest.Builder(imageLoader.defaults)
 *     .data("https://www.example.com/image.jpg")
 *     .size(1080, 1920)
 *     .build()
 * imageLoader.get(request)
 * ```
 *
 * @see GetRequestBuilder
 * @see ImageLoader.get
 */
class GetRequest internal constructor(
    override val data: Any,
    override val key: String?,
    override val aliasKeys: List<String>,
    override val listener: Listener?,
    override val sizeResolver: SizeResolver?,
    override val scale: Scale?,
    override val precision: Precision,
    override val decoder: Decoder?,
    override val dispatcher: CoroutineDispatcher,
    override val transformations: List<Transformation>,
    override val bitmapConfig: Bitmap.Config,
    override val colorSpace: ColorSpace?,
    override val headers: Headers,
    override val parameters: Parameters,
    override val networkCachePolicy: CachePolicy,
    override val diskCachePolicy: CachePolicy,
    override val memoryCachePolicy: CachePolicy,
    override val allowHardware: Boolean,
    override val allowRgb565: Boolean
) : Request() {

    companion object {
        @JvmStatic
        @JvmName("builder")
        inline fun Builder(defaults: DefaultRequestOptions) = GetRequestBuilder(defaults)

        @JvmStatic
        @JvmName("builder")
        inline fun Builder(request: GetRequest) = GetRequestBuilder(request)

        /** Create a new [GetRequest] instance. */
        @Deprecated(
            message = "Use GetRequest.Builder to create new instances.",
            replaceWith = ReplaceWith("GetRequest.Builder(defaults).apply(builder).build()")
        )
        inline operator fun invoke(
            defaults: DefaultRequestOptions,
            builder: GetRequestBuilder.() -> Unit = {}
        ): GetRequest = GetRequestBuilder(defaults).apply(builder).build()

        /** Create a new [GetRequest] instance. */
        @Deprecated(
            message = "Use GetRequest.Builder to create new instances.",
            replaceWith = ReplaceWith("GetRequest.Builder(request).apply(builder).build()")
        )
        inline operator fun invoke(
            request: GetRequest,
            builder: GetRequestBuilder.() -> Unit = {}
        ): GetRequest = GetRequestBuilder(request).apply(builder).build()
    }

    override val target: Target? = null
    override val lifecycle: Lifecycle? = null
    override val transition: Transition? = null
    override val placeholder: Drawable? = null
    override val error: Drawable? = null
    override val fallback: Drawable? = null

    /** Create a new [GetRequestBuilder] instance using this as a base. */
    fun newBuilder() = GetRequestBuilder(this)
}
