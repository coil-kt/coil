@file:Suppress("unused")
@file:OptIn(ExperimentalCoilApi::class)

package coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import coil.ComponentRegistry
import coil.DefaultRequestOptions
import coil.ImageLoader
import coil.ImageLoaderBuilder
import coil.annotation.BuilderMarker
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.Decoder
import coil.drawable.CrossfadeDrawable
import coil.fetch.Fetcher
import coil.memory.RequestService
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.target.ImageViewTarget
import coil.target.Target
import coil.transform.Transformation
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.util.EMPTY_DRAWABLE
import coil.util.Utils
import coil.util.orEmpty
import coil.util.self
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.Headers
import okhttp3.HttpUrl
import java.io.File

/** Base class for [LoadRequestBuilder] and [GetRequestBuilder]. */
@BuilderMarker
sealed class RequestBuilder<T : RequestBuilder<T>> {

    @JvmField protected var data: Any?
    @JvmField protected var key: String?
    @JvmField protected var aliasKeys: List<String>

    @JvmField protected var listener: Request.Listener?
    @JvmField protected var dispatcher: CoroutineDispatcher?
    @JvmField protected var transformations: List<Transformation>
    @JvmField protected var bitmapConfig: Bitmap.Config
    @JvmField protected var colorSpace: ColorSpace? = null

    @JvmField protected var sizeResolver: SizeResolver?
    @JvmField protected var scale: Scale?
    @JvmField protected var precision: Precision?

    @JvmField protected var fetcher: Pair<Class<*>, Fetcher<*>>?
    @JvmField protected var decoder: Decoder?

    @JvmField protected var allowHardware: Boolean?
    @JvmField protected var allowRgb565: Boolean?

    @JvmField protected var memoryCachePolicy: CachePolicy?
    @JvmField protected var diskCachePolicy: CachePolicy?
    @JvmField protected var networkCachePolicy: CachePolicy?

    @JvmField protected var headers: Headers.Builder?
    @JvmField protected var parameters: Parameters.Builder?

    constructor() {
        data = null
        key = null
        aliasKeys = emptyList()
        listener = null
        dispatcher = null
        transformations = emptyList()
        bitmapConfig = Utils.getDefaultBitmapConfig()
        if (SDK_INT >= 26) colorSpace = null
        sizeResolver = null
        scale = null
        precision = null
        fetcher = null
        decoder = null
        allowHardware = null
        allowRgb565 = null
        memoryCachePolicy = null
        diskCachePolicy = null
        networkCachePolicy = null
        headers = null
        parameters = null
    }

    constructor(request: Request) {
        data = request.data
        key = request.key
        aliasKeys = request.aliasKeys
        listener = request.listener
        dispatcher = request.dispatcher
        transformations = request.transformations
        bitmapConfig = request.bitmapConfig
        if (SDK_INT >= 26) colorSpace = request.colorSpace
        sizeResolver = request.sizeResolver
        scale = request.scale
        precision = request.precision
        fetcher = request.fetcher
        decoder = request.decoder
        allowHardware = request.allowHardware
        allowRgb565 = request.allowRgb565
        memoryCachePolicy = request.memoryCachePolicy
        diskCachePolicy = request.diskCachePolicy
        networkCachePolicy = request.networkCachePolicy
        headers = request.headers.newBuilder()
        parameters = request.parameters.newBuilder()
    }

    /**
     * Set the data to load.
     *
     * The default supported data types are:
     * - [String] (mapped to a [Uri])
     * - [HttpUrl]
     * - [Uri] ("android.resource", "content", "file", "http", and "https" schemes only)
     * - [File]
     * - @DrawableRes [Int]
     * - [Drawable]
     * - [Bitmap]
     */
    fun data(data: Any?): T = self {
        this.data = data
    }

    /**
     * Set the cache key for this request.
     *
     * By default, the cache key is computed by the [Fetcher], any [Parameters], and any [Transformation]s.
     */
    fun key(key: String?): T = self {
        this.key = key
    }

    /**
     * Set a list of supplementary cache keys that are used to check if this request is cached in the memory cache.
     *
     * Requests are still written to the memory cache as [key].
     */
    fun aliasKeys(vararg aliasKeys: String): T = self {
        this.aliasKeys = aliasKeys.toList()
    }

    /**
     * Set a list of supplementary cache keys that are used to check if this request is cached in the memory cache.
     *
     * Requests are still written to the memory cache as [key].
     */
    fun aliasKeys(aliasKeys: List<String>): T = self {
        this.aliasKeys = aliasKeys.toList()
    }

    /**
     * Convenience function to create and set the [Request.Listener].
     */
    inline fun listener(
        crossinline onStart: (request: Request) -> Unit = {},
        crossinline onCancel: (request: Request) -> Unit = {},
        crossinline onError: (request: Request, throwable: Throwable) -> Unit = { _, _ -> },
        crossinline onSuccess: (request: Request, source: DataSource) -> Unit = { _, _ -> }
    ): T = listener(object : Request.Listener {
        override fun onStart(request: Request) = onStart(request)
        override fun onCancel(request: Request) = onCancel(request)
        override fun onError(request: Request, throwable: Throwable) = onError(request, throwable)
        override fun onSuccess(request: Request, source: DataSource) = onSuccess(request, source)
    })

    /**
     * Set the [Request.Listener].
     */
    fun listener(listener: Request.Listener?): T = self {
        this.listener = listener
    }

    /**
     * Set the [CoroutineDispatcher] to run the fetching, decoding, and transforming work on.
     */
    fun dispatcher(dispatcher: CoroutineDispatcher): T = self {
        this.dispatcher = dispatcher
    }

    /**
     * Set the list of [Transformation]s to be applied to this request.
     */
    fun transformations(vararg transformations: Transformation): T = self {
        this.transformations = transformations.toList()
    }

    /**
     * Set the list of [Transformation]s to be applied to this request.
     */
    fun transformations(transformations: List<Transformation>): T = self {
        this.transformations = transformations.toList()
    }

    /**
     * Set the preferred [Bitmap.Config].
     *
     * This is not guaranteed and a different config may be used in some situations.
     */
    fun bitmapConfig(bitmapConfig: Bitmap.Config): T = self {
        this.bitmapConfig = bitmapConfig
    }

    /**
     * Set the preferred [ColorSpace].
     */
    @RequiresApi(26)
    fun colorSpace(colorSpace: ColorSpace): T = self {
        this.colorSpace = colorSpace
    }

    /**
     * Set the requested width/height.
     */
    fun size(@Px size: Int): T = self {
        size(size, size)
    }

    /**
     * Set the requested width/height.
     */
    fun size(@Px width: Int, @Px height: Int): T = self {
        size(PixelSize(width, height))
    }

    /**
     * Set the requested width/height.
     */
    fun size(size: Size): T = self {
        this.sizeResolver = SizeResolver(size)
    }

    /**
     * Set the [SizeResolver] for this request. It will be used to determine the requested width/height for this request.
     *
     * If this isn't set, Coil will attempt to determine the size of the request using the logic in [RequestService.sizeResolver].
     */
    fun size(resolver: SizeResolver): T = self {
        this.sizeResolver = resolver
    }

    /**
     * Set the scaling algorithm that will be used to fit/fill the image into the dimensions provided by [sizeResolver].
     *
     * If this isn't set, Coil will attempt to determine the scale of the request using the logic in [RequestService.scale].
     *
     * NOTE: If [scale] is not set, it is automatically computed for [ImageView] targets.
     */
    fun scale(scale: Scale): T = self {
        this.scale = scale
    }

    /**
     * Set the required precision for the size of the loaded image.
     *
     * The default value is [Precision.AUTOMATIC], which uses the logic in [RequestService.allowInexactSize]
     * to determine if output image's dimensions must match the input [size] and [scale] exactly.
     *
     * NOTE: If [size] is [OriginalSize], image's dimensions will always be equal to or greater than
     * the image's original dimensions.
     *
     * @see Precision
     */
    fun precision(precision: Precision): T = self {
        this.precision = precision
    }

    /**
     * Set the [Fetcher] to handle fetching any image data.
     *
     * If this isn't set, the [ImageLoader] will find an applicable [Fetcher] that's registered in its [ComponentRegistry].
     *
     * NOTE: This skips calling [Fetcher.handles] for [fetcher].
     */
    inline fun <reified R : Any> fetcher(fetcher: Fetcher<R>) = fetcher(R::class.java, fetcher)

    /**
     * @see RequestBuilder.fetcher
     */
    @PublishedApi
    internal fun <R : Any> fetcher(type: Class<R>, fetcher: Fetcher<R>): T = self {
        this.fetcher = type to fetcher
    }

    /**
     * Set the [Decoder] to handle decoding any image data.
     *
     * If this isn't set, the [ImageLoader] will find an applicable [Decoder] that's registered in its [ComponentRegistry].
     *
     * NOTE: This skips calling [Decoder.handles] for [decoder].
     */
    fun decoder(decoder: Decoder): T = self {
        this.decoder = decoder
    }

    /**
     * Enable/disable the use of [Bitmap.Config.HARDWARE] for this request.
     *
     * If false, any use of [Bitmap.Config.HARDWARE] will be treated as [Bitmap.Config.ARGB_8888].
     *
     * This is useful for shared element transitions, which do not support hardware bitmaps.
     */
    fun allowHardware(enable: Boolean): T = self {
        this.allowHardware = enable
    }

    /**
     * See: [ImageLoaderBuilder.allowRgb565]
     */
    fun allowRgb565(enable: Boolean): T = self {
        this.allowRgb565 = enable
    }

    /**
     * Enable/disable reading/writing from/to the memory cache.
     */
    fun memoryCachePolicy(policy: CachePolicy): T = self {
        this.memoryCachePolicy = policy
    }

    /**
     * Enable/disable reading/writing from/to the disk cache.
     */
    fun diskCachePolicy(policy: CachePolicy): T = self {
        this.diskCachePolicy = policy
    }

    /**
     * Enable/disable reading from the network.
     *
     * NOTE: Disabling writes has no effect.
     */
    fun networkCachePolicy(policy: CachePolicy): T = self {
        this.networkCachePolicy = policy
    }

    /**
     * Set the [Headers] for any network operations performed by this request.
     */
    fun headers(headers: Headers): T = self {
        this.headers = headers.newBuilder()
    }

    /**
     * Add a header for any network operations performed by this request.
     *
     * @see Headers.Builder.add
     */
    fun addHeader(name: String, value: String): T = self {
        this.headers = (this.headers ?: Headers.Builder()).add(name, value)
    }

    /**
     * Set a header for any network operations performed by this request.
     *
     * @see Headers.Builder.set
     */
    fun setHeader(name: String, value: String): T = self {
        this.headers = (this.headers ?: Headers.Builder()).set(name, value)
    }

    /**
     * Remove all network headers with the key [name].
     */
    fun removeHeader(name: String): T = self {
        this.headers = this.headers?.removeAll(name)
    }

    /**
     * Set the parameters for this request.
     */
    fun parameters(parameters: Parameters): T = self {
        this.parameters = parameters.newBuilder()
    }

    /**
     * Set a parameter for this request.
     *
     * @see Parameters.Builder.set
     */
    @JvmOverloads
    fun setParameter(key: String, value: Any?, cacheKey: String? = value?.toString()): T = self {
        this.parameters = (this.parameters ?: Parameters.Builder()).apply { set(key, value, cacheKey) }
    }

    /**
     * Remove a parameter from this request.
     *
     * @see Parameters.Builder.remove
     */
    fun removeParameter(key: String): T = self {
        this.parameters?.remove(key)
    }
}

/** Builder for a [LoadRequest]. */
class LoadRequestBuilder : RequestBuilder<LoadRequestBuilder> {

    private val context: Context

    private var target: Target?
    private var lifecycle: Lifecycle?
    private var transition: Transition?

    @DrawableRes private var placeholderResId: Int
    @DrawableRes private var errorResId: Int
    @DrawableRes private var fallbackResId: Int
    private var placeholderDrawable: Drawable?
    private var errorDrawable: Drawable?
    private var fallbackDrawable: Drawable?

    constructor(context: Context) : super() {
        this.context = context
        target = null
        lifecycle = null
        transition = null
        placeholderResId = 0
        errorResId = 0
        fallbackResId = 0
        placeholderDrawable = null
        errorDrawable = null
        fallbackDrawable = null
    }

    @JvmOverloads
    constructor(
        request: LoadRequest,
        context: Context = request.context
    ) : super(request) {
        this.context = context
        target = request.target
        lifecycle = request.lifecycle
        transition = request.transition
        placeholderResId = request.placeholderResId
        errorResId = request.errorResId
        fallbackResId = request.fallbackResId
        placeholderDrawable = request.placeholderDrawable
        errorDrawable = request.errorDrawable
        fallbackDrawable = request.fallbackDrawable
    }

    @Deprecated(
        message = "Migrate to LoadRequest.Builder(context).",
        replaceWith = ReplaceWith("LoadRequest.Builder(context)")
    )
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context, defaults: DefaultRequestOptions) : this(context)

    /**
     * Convenience function to set [imageView] as the [Target].
     */
    fun target(imageView: ImageView) = apply {
        target(ImageViewTarget(imageView))
    }

    /**
     * Convenience function to create and set the [Target].
     */
    inline fun target(
        crossinline onStart: (placeholder: Drawable?) -> Unit = {},
        crossinline onError: (error: Drawable?) -> Unit = {},
        crossinline onSuccess: (result: Drawable) -> Unit = {}
    ) = target(object : Target {
        override fun onStart(placeholder: Drawable?) = onStart(placeholder)
        override fun onError(error: Drawable?) = onError(error)
        override fun onSuccess(result: Drawable) = onSuccess(result)
    })

    /**
     * Set the [Target]. If the target is null, this request will preload the image into memory.
     */
    fun target(target: Target?) = apply {
        this.target = target
    }

    /**
     * Set the [Lifecycle] for this request.
     */
    fun lifecycle(owner: LifecycleOwner?) = apply {
        lifecycle(owner?.lifecycle)
    }

    /**
     * Set the [Lifecycle] for this request.
     *
     * Requests are queued while the lifecycle is not at least [Lifecycle.State.STARTED].
     * Requests are cancelled when the lifecycle reaches [Lifecycle.State.DESTROYED].
     *
     * If this isn't set, Coil will attempt to find the lifecycle for this request
     * using the logic in [RequestService.lifecycleInfo].
     */
    fun lifecycle(lifecycle: Lifecycle?) = apply {
        this.lifecycle = lifecycle
    }

    /**
     * See: [ImageLoaderBuilder.crossfade]
     */
    fun crossfade(enable: Boolean) = crossfade(if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0)

    /**
     * See: [ImageLoaderBuilder.crossfade]
     */
    fun crossfade(durationMillis: Int) = apply {
        this.transition = if (durationMillis > 0) CrossfadeTransition(durationMillis) else null
    }

    /**
     * See: [ImageLoaderBuilder.transition]
     */
    @ExperimentalCoilApi
    fun transition(transition: Transition) = apply {
        this.transition = transition
    }

    /**
     * Set the placeholder drawable to use when the request starts.
     */
    fun placeholder(@DrawableRes drawableResId: Int) = apply {
        this.placeholderResId = drawableResId
        this.placeholderDrawable = EMPTY_DRAWABLE
    }

    /**
     * Set the placeholder drawable to use when the request starts.
     */
    fun placeholder(drawable: Drawable?) = apply {
        this.placeholderDrawable = drawable ?: EMPTY_DRAWABLE
        this.placeholderResId = 0
    }

    /**
     * Set the error drawable to use if the request fails.
     */
    fun error(@DrawableRes drawableResId: Int) = apply {
        this.errorResId = drawableResId
        this.errorDrawable = EMPTY_DRAWABLE
    }

    /**
     * Set the error drawable to use if the request fails.
     */
    fun error(drawable: Drawable?) = apply {
        this.errorDrawable = drawable ?: EMPTY_DRAWABLE
        this.errorResId = 0
    }

    /**
     * Set the fallback drawable to use if [data] is null.
     */
    fun fallback(@DrawableRes drawableResId: Int) = apply {
        this.fallbackResId = drawableResId
        this.fallbackDrawable = EMPTY_DRAWABLE
    }

    /**
     * Set the fallback drawable to use if [data] is null.
     */
    fun fallback(drawable: Drawable?) = apply {
        this.fallbackDrawable = drawable ?: EMPTY_DRAWABLE
        this.fallbackResId = 0
    }

    /**
     * Create a new [LoadRequest] instance.
     */
    fun build(): LoadRequest {
        return LoadRequest(
            context,
            data,
            key,
            aliasKeys,
            listener,
            dispatcher,
            transformations,
            bitmapConfig,
            colorSpace,
            sizeResolver,
            scale,
            precision,
            fetcher,
            decoder,
            allowHardware,
            allowRgb565,
            memoryCachePolicy,
            diskCachePolicy,
            networkCachePolicy,
            headers?.build().orEmpty(),
            parameters?.build().orEmpty(),
            target,
            lifecycle,
            transition,
            placeholderResId,
            errorResId,
            fallbackResId,
            placeholderDrawable,
            errorDrawable,
            fallbackDrawable
        )
    }
}

/** Builder for a [GetRequest]. */
class GetRequestBuilder : RequestBuilder<GetRequestBuilder> {

    constructor() : super()

    constructor(request: GetRequest) : super(request)

    @Deprecated(
        message = "Migrate to GetRequest.Builder().",
        replaceWith = ReplaceWith("GetRequest.Builder()")
    )
    @Suppress("UNUSED_PARAMETER")
    constructor(defaults: DefaultRequestOptions) : this()

    /**
     * Create a new [GetRequest] instance.
     */
    fun build(): GetRequest {
        return GetRequest(
            data,
            key,
            aliasKeys,
            listener,
            dispatcher,
            transformations,
            bitmapConfig,
            colorSpace,
            sizeResolver,
            scale,
            precision,
            fetcher,
            decoder,
            allowHardware,
            allowRgb565,
            memoryCachePolicy,
            diskCachePolicy,
            networkCachePolicy,
            headers?.build().orEmpty(),
            parameters?.build().orEmpty()
        )
    }
}
