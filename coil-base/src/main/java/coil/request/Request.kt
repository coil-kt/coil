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
import coil.fetch.Fetcher
import coil.size.Precision
import coil.size.Scale
import coil.size.SizeResolver
import coil.target.PoolableViewTarget
import coil.target.Target
import coil.transform.Transformation
import coil.transition.Transition
import coil.util.EMPTY_DRAWABLE
import coil.util.getDrawableCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import okhttp3.Headers

/**
 * The base class for an image request.
 *
 * There are two types of image requests: [LoadRequest]s and [GetRequest]s.
 */
sealed class Request {

    abstract val data: Any?
    abstract val key: String?
    abstract val aliasKeys: List<String>

    abstract val listener: Listener?
    abstract val dispatcher: CoroutineDispatcher?
    abstract val transformations: List<Transformation>
    abstract val bitmapConfig: Bitmap.Config
    abstract val colorSpace: ColorSpace?

    abstract val sizeResolver: SizeResolver?
    abstract val scale: Scale?
    abstract val precision: Precision?

    abstract val fetcher: Fetcher<*>?
    abstract val decoder: Decoder?

    abstract val allowHardware: Boolean?
    abstract val allowRgb565: Boolean?

    abstract val memoryCachePolicy: CachePolicy?
    abstract val diskCachePolicy: CachePolicy?
    abstract val networkCachePolicy: CachePolicy?

    abstract val headers: Headers
    abstract val parameters: Parameters

    abstract val target: Target?
    abstract val lifecycle: Lifecycle?
    abstract val transition: Transition?

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
        fun onStart(request: Request) {}

        /**
         * Called when the request successfully loads the image.
         */
        @MainThread
        fun onSuccess(request: Request, source: DataSource) {}

        /**
         * Called when the request is cancelled.
         */
        @MainThread
        fun onCancel(request: Request) {}

        /**
         * Called when the request fails to load the image.
         */
        @MainThread
        fun onError(request: Request, throwable: Throwable) {}
    }
}

/**
 * [LoadRequest]s asynchronously load an image into a [Target].
 *
 * [Request.data] must be set to a non-null value or the request
 * will fail with [NullRequestDataException] when executed.
 *
 * - They are scoped to a [Lifecycle]. Requests aren't started until the lifecycle is
 *   started and are automatically cancelled when the lifecycle is destroyed.
 * - They support [placeholder], [error], and [fallback] drawables.
 * - They support [Transition]s.
 * - They support bitmap pooling (if [target] implements [PoolableViewTarget]).
 *
 * Example:
 * ```
 * val request = LoadRequest.Builder(context)
 *     .data("https://www.example.com/image.jpg")
 *     .target(imageView)
 *     .build()
 * val disposable = imageLoader.launch(request)
 * ```
 *
 * @see LoadRequestBuilder
 * @see ImageLoader.launch
 */
class LoadRequest internal constructor(
    val context: Context,
    override val data: Any?,
    override val key: String?,
    override val aliasKeys: List<String>,
    override val listener: Listener?,
    override val dispatcher: CoroutineDispatcher?,
    override val transformations: List<Transformation>,
    override val bitmapConfig: Bitmap.Config,
    override val colorSpace: ColorSpace?,
    override val sizeResolver: SizeResolver?,
    override val scale: Scale?,
    override val precision: Precision?,
    override val fetcher: Fetcher<*>?,
    override val decoder: Decoder?,
    override val allowHardware: Boolean?,
    override val allowRgb565: Boolean?,
    override val memoryCachePolicy: CachePolicy?,
    override val diskCachePolicy: CachePolicy?,
    override val networkCachePolicy: CachePolicy?,
    override val headers: Headers,
    override val parameters: Parameters,
    override val target: Target?,
    override val lifecycle: Lifecycle?,
    override val transition: Transition?,
    @DrawableRes internal val placeholderResId: Int,
    @DrawableRes internal val errorResId: Int,
    @DrawableRes internal val fallbackResId: Int,
    internal val placeholderDrawable: Drawable?,
    internal val errorDrawable: Drawable?,
    internal val fallbackDrawable: Drawable?
) : Request() {

    companion object {
        /** Alias for [LoadRequestBuilder]. */
        @JvmStatic
        @JvmName("builder")
        inline fun Builder(context: Context) = LoadRequestBuilder(context)

        /** Alias for [LoadRequestBuilder]. */
        @JvmStatic
        @JvmOverloads
        @JvmName("builder")
        inline fun Builder(
            request: LoadRequest,
            context: Context = request.context
        ) = LoadRequestBuilder(request, context)

        /** Create a new [LoadRequest]. */
        @Deprecated(
            message = "Use LoadRequest.Builder to create new instances.",
            replaceWith = ReplaceWith("LoadRequest.Builder(context).apply(builder).build()")
        )
        @Suppress("UNUSED_PARAMETER")
        inline operator fun invoke(
            context: Context,
            defaults: DefaultRequestOptions,
            builder: LoadRequestBuilder.() -> Unit = {}
        ): LoadRequest = LoadRequestBuilder(context).apply(builder).build()

        /** Create a new [LoadRequest]. */
        @Deprecated(
            message = "Use LoadRequest.Builder to create new instances.",
            replaceWith = ReplaceWith("LoadRequest.Builder(request, context).apply(builder).build()")
        )
        inline operator fun invoke(
            context: Context,
            request: LoadRequest,
            builder: LoadRequestBuilder.() -> Unit = {}
        ): LoadRequest = LoadRequestBuilder(request, context).apply(builder).build()
    }

    override val placeholder: Drawable?
        get() = context.getDrawable(placeholderDrawable, placeholderResId)

    override val error: Drawable?
        get() = context.getDrawable(errorDrawable, errorResId)

    override val fallback: Drawable?
        get() = context.getDrawable(fallbackDrawable, fallbackResId)

    private fun Context.getDrawable(drawable: Drawable?, @DrawableRes resId: Int): Drawable? {
        return drawable.takeIf { it !== EMPTY_DRAWABLE } ?: if (resId != 0) getDrawableCompat(resId) else null
    }

    /** Create a new [LoadRequestBuilder] instance using this as a base. */
    @JvmOverloads
    fun newBuilder(context: Context = this.context) = LoadRequestBuilder(this, context)
}

/**
 * [GetRequest]s suspend the current coroutine and return the drawable directly to the caller.
 *
 * [Request.data] must be set to a non-null value or the request
 * will fail with [NullRequestDataException] when executed.
 *
 * - They are scoped to the [CoroutineScope] that they are launched in. They are **not** scoped to a [Lifecycle].
 * - They do not support a [Target], [Transition], [placeholder], [error], or [fallback].
 *
 * Example:
 * ```
 * val request = GetRequest.Builder()
 *     .data("https://www.example.com/image.jpg")
 *     .size(256, 256)
 *     .build()
 * val drawable = imageLoader.launch(request)
 * ```
 *
 * @see GetRequestBuilder
 * @see ImageLoader.get
 */
class GetRequest internal constructor(
    override val data: Any?,
    override val key: String?,
    override val aliasKeys: List<String>,
    override val listener: Listener?,
    override val dispatcher: CoroutineDispatcher?,
    override val transformations: List<Transformation>,
    override val bitmapConfig: Bitmap.Config,
    override val colorSpace: ColorSpace?,
    override val sizeResolver: SizeResolver?,
    override val scale: Scale?,
    override val precision: Precision?,
    override val fetcher: Fetcher<*>?,
    override val decoder: Decoder?,
    override val allowHardware: Boolean?,
    override val allowRgb565: Boolean?,
    override val memoryCachePolicy: CachePolicy?,
    override val diskCachePolicy: CachePolicy?,
    override val networkCachePolicy: CachePolicy?,
    override val headers: Headers,
    override val parameters: Parameters
) : Request() {

    companion object {
        /** Alias for [GetRequestBuilder]. */
        @JvmStatic
        @JvmName("builder")
        inline fun Builder() = GetRequestBuilder()

        /** Alias for [GetRequestBuilder]. */
        @JvmStatic
        @JvmName("builder")
        inline fun Builder(request: GetRequest) = GetRequestBuilder(request)

        /** Create a new [GetRequest]. */
        @Deprecated(
            message = "Use GetRequest.Builder to create new instances.",
            replaceWith = ReplaceWith("GetRequest.Builder(defaults).apply(builder).build()")
        )
        @Suppress("UNUSED_PARAMETER")
        inline operator fun invoke(
            defaults: DefaultRequestOptions,
            builder: GetRequestBuilder.() -> Unit = {}
        ): GetRequest = GetRequestBuilder().apply(builder).build()

        /** Create a new [GetRequest]. */
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
