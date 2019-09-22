@file:Suppress("unused")

package coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import coil.DefaultRequestOptions
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.Decoder
import coil.size.Scale
import coil.size.SizeResolver
import coil.target.Target
import coil.transform.Transformation
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
    abstract val crossfadeMillis: Int

    abstract val key: String?
    abstract val listener: Listener?
    abstract val sizeResolver: SizeResolver?
    abstract val scale: Scale?
    abstract val decoder: Decoder?
    abstract val dispatcher: CoroutineDispatcher
    abstract val transformations: List<Transformation>
    abstract val bitmapConfig: Bitmap.Config
    abstract val colorSpace: ColorSpace?
    abstract val headers: Headers

    abstract val networkCachePolicy: CachePolicy
    abstract val diskCachePolicy: CachePolicy
    abstract val memoryCachePolicy: CachePolicy

    abstract val allowHardware: Boolean
    abstract val allowRgb565: Boolean

    abstract val placeholder: Drawable?
    abstract val error: Drawable?

    /**
     * A set of callbacks for a [Request]. All callbacks are guaranteed to be called on the main thread.
     */
    interface Listener {

        /**
         * Called when the request is dispatched and starts loading the image.
         */
        fun onStart(data: Any) {}

        /**
         * Called when the request successfully loads the image.
         */
        fun onSuccess(data: Any, source: DataSource) {}

        /**
         * Called when the request is cancelled.
         */
        fun onCancel(data: Any) {}

        /**
         * Called when the request fails to load the image.
         */
        fun onError(data: Any, throwable: Throwable) {}
    }
}

/**
 * A value object that represents a *load* image request.
 *
 * Instances can be created ad hoc:
 * ```
 * imageLoader.load(context, "https://www.example.com/image.jpg") {
 *     crossfade(true)
 *     target(imageView)
 * }
 * ```
 *
 * Or instances can be created separately from the call that executes them:
 * ```
 * val request = LoadRequest(context, imageLoader.defaults) {
 *     data("https://www.example.com/image.jpg")
 *     crossfade(true)
 *     target(imageView)
 * }
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
    override val crossfadeMillis: Int,
    override val key: String?,
    override val listener: Listener?,
    override val sizeResolver: SizeResolver?,
    override val scale: Scale?,
    override val decoder: Decoder?,
    override val dispatcher: CoroutineDispatcher,
    override val transformations: List<Transformation>,
    override val bitmapConfig: Bitmap.Config,
    override val colorSpace: ColorSpace?,
    override val headers: Headers,
    override val networkCachePolicy: CachePolicy,
    override val diskCachePolicy: CachePolicy,
    override val memoryCachePolicy: CachePolicy,
    override val allowHardware: Boolean,
    override val allowRgb565: Boolean,
    @DrawableRes internal val placeholderResId: Int,
    @DrawableRes internal val errorResId: Int,
    internal val placeholderDrawable: Drawable?,
    internal val errorDrawable: Drawable?
) : Request() {

    companion object {
        /** Create a new [LoadRequest] instance. */
        inline operator fun invoke(
            context: Context,
            defaults: DefaultRequestOptions,
            builder: LoadRequestBuilder.() -> Unit = {}
        ): LoadRequest = LoadRequestBuilder(context, defaults).apply(builder).build()

        /** Create a new [LoadRequest] instance. */
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
 * Instances can be created ad hoc:
 * ```
 * val drawable = imageLoader.get("https://www.example.com/image.jpg") {
 *     size(1080, 1920)
 * }
 * ```
 *
 * Or instances can be created separately from the call that executes them:
 * ```
 * val request = GetRequest(imageLoader.defaults) {
 *     data("https://www.example.com/image.jpg")
 *     size(1080, 1920)
 * }
 * imageLoader.get(request)
 * ```
 *
 * @see GetRequestBuilder
 * @see ImageLoader.get
 */
class GetRequest internal constructor(
    override val data: Any,
    override val key: String?,
    override val listener: Listener?,
    override val sizeResolver: SizeResolver?,
    override val scale: Scale?,
    override val decoder: Decoder?,
    override val dispatcher: CoroutineDispatcher,
    override val transformations: List<Transformation>,
    override val bitmapConfig: Bitmap.Config,
    override val colorSpace: ColorSpace?,
    override val headers: Headers,
    override val networkCachePolicy: CachePolicy,
    override val diskCachePolicy: CachePolicy,
    override val memoryCachePolicy: CachePolicy,
    override val allowHardware: Boolean,
    override val allowRgb565: Boolean
) : Request() {

    companion object {
        /** Create a new [GetRequest] instance. */
        inline operator fun invoke(
            defaults: DefaultRequestOptions,
            builder: GetRequestBuilder.() -> Unit = {}
        ): GetRequest = GetRequestBuilder(defaults).apply(builder).build()

        /** Create a new [GetRequest] instance. */
        inline operator fun invoke(
            request: GetRequest,
            builder: GetRequestBuilder.() -> Unit = {}
        ): GetRequest = GetRequestBuilder(request).apply(builder).build()
    }

    override val target: Target? = null

    override val lifecycle: Lifecycle? = null

    override val crossfadeMillis: Int = 0

    override val placeholder: Drawable? = null

    override val error: Drawable? = null

    /** Create a new [GetRequestBuilder] instance using this as a base. */
    fun newBuilder() = GetRequestBuilder(this)
}
