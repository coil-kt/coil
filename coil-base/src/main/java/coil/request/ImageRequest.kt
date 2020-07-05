@file:OptIn(ExperimentalCoilApi::class)
@file:Suppress("unused")

package coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import coil.ComponentRegistry
import coil.DefaultRequestOptions
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.Decoder
import coil.drawable.CrossfadeDrawable
import coil.fetch.Fetcher
import coil.memory.MemoryCache
import coil.request.ImageRequest.Builder
import coil.size.DisplaySizeResolver
import coil.size.OriginalSize
import coil.size.PixelSize
import coil.size.Precision
import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver
import coil.size.ViewSizeResolver
import coil.target.ImageViewTarget
import coil.target.Target
import coil.target.ViewTarget
import coil.transform.Transformation
import coil.transition.CrossfadeTransition
import coil.transition.Transition
import coil.util.getDrawableCompat
import coil.util.getLifecycle
import coil.util.orEmpty
import coil.util.scale
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.Headers
import okhttp3.HttpUrl
import java.io.File

/**
 * An immutable data object that represents a request for an image.
 *
 * @see ImageLoader.enqueue
 * @see ImageLoader.execute
 */
class ImageRequest private constructor(
    val context: Context,

    /** @see Builder.data */
    val data: Any,

    /** @see Builder.target */
    val target: Target?,

    /** @see Builder.listener */
    val listener: Listener?,

    /** @see Builder.key */
    val key: MemoryCache.Key?,

    /** @see Builder.placeholderKey */
    val placeholderKey: MemoryCache.Key?,

    /** @see Builder.colorSpace */
    val colorSpace: ColorSpace?,

    /** @see Builder.fetcher */
    val fetcher: Pair<Fetcher<*>, Class<*>>?,

    /** @see Builder.decoder */
    val decoder: Decoder?,

    /** @see Builder.transformations */
    val transformations: List<Transformation>,

    /** @see Builder.headers */
    val headers: Headers,

    /** @see Builder.parameters */
    val parameters: Parameters,

    // Resolved lazily if not set while building.
    private val _lifecycle: Lifecycle?,
    private val _sizeResolver: SizeResolver?,
    private val _scale: Scale?,

    // Merged with defaults.
    private val _dispatcher: CoroutineDispatcher?,
    private val _transition: Transition?,
    private val _precision: Precision?,
    private val _bitmapConfig: Bitmap.Config?,
    private val _allowHardware: Boolean?,
    private val _allowRgb565: Boolean?,
    private val _placeholderResId: Int?,
    private val _placeholderDrawable: Drawable?,
    private val _errorResId: Int?,
    private val _errorDrawable: Drawable?,
    private val _fallbackResId: Int?,
    private val _fallbackDrawable: Drawable?,
    private val _memoryCachePolicy: CachePolicy?,
    private val _diskCachePolicy: CachePolicy?,
    private val _networkCachePolicy: CachePolicy?,

    /** @see Builder.defaults */
    val defaults: DefaultRequestOptions
) {

    private var lazyLifecycle: Lifecycle? = _lifecycle
    private var lazySizeResolver: SizeResolver? = _sizeResolver
    private var lazyScale: Scale? = _scale
    private var lazyAllowInexactSize: Boolean? = null

    /** @see Builder.lifecycle */
    val lifecycle: Lifecycle get() = lazyLifecycle ?: resolveLifecycle().also { lazyLifecycle = it }

    /** @see Builder.sizeResolver */
    val sizeResolver: SizeResolver get() = lazySizeResolver ?: resolveSizeResolver().also { lazySizeResolver = it }

    /** @see Builder.scale */
    val scale: Scale get() = lazyScale ?: resolveScale().also { lazyScale = it }

    val allowInexactSize: Boolean get() = lazyAllowInexactSize ?: resolveAllowInexactSize().also { lazyAllowInexactSize = it }

    /** @see Builder.dispatcher */
    val dispatcher: CoroutineDispatcher = _dispatcher ?: defaults.dispatcher

    /** @see Builder.transition */
    val transition: Transition = _transition ?: defaults.transition

    /** @see Builder.precision */
    val precision: Precision = _precision ?: defaults.precision

    /** @see Builder.bitmapConfig */
    val bitmapConfig: Bitmap.Config = _bitmapConfig ?: defaults.bitmapConfig

    /** @see Builder.allowHardware */
    val allowHardware: Boolean = _allowHardware ?: defaults.allowHardware

    /** @see Builder.allowHardware */
    val allowRgb565: Boolean = _allowRgb565 ?: defaults.allowRgb565

    /** @see Builder.placeholder */
    val placeholder: Drawable? = getDrawableCompat(_placeholderDrawable, _placeholderResId, defaults.placeholder)

    /** @see Builder.error */
    val error: Drawable? = getDrawableCompat(_errorDrawable, _errorResId, defaults.error)

    /** @see Builder.fallback */
    val fallback: Drawable? = getDrawableCompat(_fallbackDrawable, _fallbackResId, defaults.fallback)

    /** @see Builder.memoryCachePolicy */
    val memoryCachePolicy: CachePolicy = _memoryCachePolicy ?: defaults.memoryCachePolicy

    /** @see Builder.diskCachePolicy */
    val diskCachePolicy: CachePolicy = _diskCachePolicy ?: defaults.diskCachePolicy

    /** @see Builder.networkCachePolicy */
    val networkCachePolicy: CachePolicy = _networkCachePolicy ?: defaults.networkCachePolicy

    @JvmOverloads
    fun newBuilder(context: Context = this.context) = Builder(this, context)

    private fun resolveLifecycle(): Lifecycle {
        val context = if (target is ViewTarget<*>) target.view.context else context
        return context.getLifecycle() ?: GlobalLifecycle
    }

    private fun resolveSizeResolver(): SizeResolver {
        return if (target is ViewTarget<*>) ViewSizeResolver(target.view) else DisplaySizeResolver(context)
    }

    private fun resolveScale(): Scale {
        val sizeResolver = sizeResolver
        if (sizeResolver is ViewSizeResolver<*>) {
            val view = sizeResolver.view
            if (view is ImageView) return view.scale
        }

        val target = target
        if (target is ViewTarget<*>) {
            val view = target.view
            if (view is ImageView) return view.scale
        }

        return Scale.FIT
    }

    private fun resolveAllowInexactSize(): Boolean {
        // If both our target and size resolver reference the same ImageView, allow the
        // dimensions to be inexact as the ImageView will scale the output image automatically.
        val target = target
        if (target is ViewTarget<*> && target.view is ImageView) {
            val sizeResolver = sizeResolver
            if (sizeResolver is ViewSizeResolver<*> && sizeResolver.view === target.view) return true
        }

        // If we implicitly fall back to a DisplaySizeResolver, allow the dimensions to be inexact.
        if (_sizeResolver == null && sizeResolver is DisplaySizeResolver) return true

        // Else, require the dimensions to be exact.
        return false
    }

    /**
     * A set of callbacks for a [ImageRequest].
     */
    interface Listener {

        /**
         * Called immediately after [Target.onStart].
         */
        @MainThread
        fun onStart(request: ImageRequest) {}

        /**
         * Called if the request completes successfully.
         */
        @MainThread
        fun onSuccess(request: ImageRequest, metadata: Metadata) {}

        /**
         * Called if the request is cancelled.
         */
        @MainThread
        fun onCancel(request: ImageRequest) {}

        /**
         * Called if an error occurs while executing the request.
         */
        @MainThread
        fun onError(request: ImageRequest, throwable: Throwable) {}
    }

    class Builder {

        private val context: Context
        private var defaults: DefaultRequestOptions
        private var data: Any?

        private var target: Target?
        private var listener: Listener?
        private var key: MemoryCache.Key?
        private var placeholderKey: MemoryCache.Key?
        private var colorSpace: ColorSpace? = null
        private var fetcher: Pair<Fetcher<*>, Class<*>>?
        private var decoder: Decoder?
        private var transformations: List<Transformation>

        private var headers: Headers.Builder?
        private var parameters: Parameters.Builder?

        private var lifecycle: Lifecycle?
        private var sizeResolver: SizeResolver?
        private var scale: Scale?

        private var dispatcher: CoroutineDispatcher?
        private var transition: Transition?
        private var precision: Precision?
        private var bitmapConfig: Bitmap.Config?
        private var allowHardware: Boolean?
        private var allowRgb565: Boolean?
        private var memoryCachePolicy: CachePolicy?
        private var diskCachePolicy: CachePolicy?
        private var networkCachePolicy: CachePolicy?

        @DrawableRes private var placeholderResId: Int?
        private var placeholderDrawable: Drawable?
        @DrawableRes private var errorResId: Int?
        private var errorDrawable: Drawable?
        @DrawableRes private var fallbackResId: Int?
        private var fallbackDrawable: Drawable?

        constructor(context: Context) {
            this.context = context
            defaults = DefaultRequestOptions.INSTANCE
            data = null
            target = null
            listener = null
            key = null
            placeholderKey = null
            if (SDK_INT >= 26) colorSpace = null
            fetcher = null
            decoder = null
            transformations = emptyList()
            headers = null
            parameters = null
            lifecycle = null
            sizeResolver = null
            scale = null
            dispatcher = null
            transition = null
            precision = null
            bitmapConfig = null
            allowHardware = null
            allowRgb565 = null
            memoryCachePolicy = null
            diskCachePolicy = null
            networkCachePolicy = null
            placeholderResId = null
            placeholderDrawable = null
            errorResId = null
            errorDrawable = null
            fallbackResId = null
            fallbackDrawable = null
        }

        @JvmOverloads
        constructor(request: ImageRequest, context: Context = request.context) {
            this.context = context
            defaults = request.defaults
            data = request.data
            target = request.target
            listener = request.listener
            key = request.key
            placeholderKey = request.placeholderKey
            if (SDK_INT >= 26) colorSpace = request.colorSpace
            fetcher = request.fetcher
            decoder = request.decoder
            transformations = request.transformations
            headers = request.headers.newBuilder()
            parameters = request.parameters.newBuilder()
            lifecycle = request._lifecycle
            sizeResolver = request._sizeResolver
            scale = request._scale
            dispatcher = request._dispatcher
            transition = request._transition
            precision = request._precision
            bitmapConfig = request._bitmapConfig
            allowHardware = request._allowHardware
            allowRgb565 = request._allowRgb565
            memoryCachePolicy = request._memoryCachePolicy
            diskCachePolicy = request._diskCachePolicy
            networkCachePolicy = request._networkCachePolicy
            placeholderResId = request._placeholderResId
            placeholderDrawable = request._placeholderDrawable
            errorResId = request._errorResId
            errorDrawable = request._errorDrawable
            fallbackResId = request._fallbackResId
            fallbackDrawable = request._fallbackDrawable
        }

        /**
         * Set the data to load.
         *
         * The default supported data types are:
         * - [String] (mapped to a [Uri])
         * - [Uri] ("android.resource", "content", "file", "http", and "https" schemes only)
         * - [HttpUrl]
         * - [File]
         * - [DrawableRes]
         * - [Drawable]
         * - [Bitmap]
         */
        fun data(data: Any?) = apply {
            this.data = data
        }

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set the [ImageLoader] will compute a memory cache key.
         */
        fun key(key: String?) = key(key?.let { MemoryCache.Key(it) })

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set the [ImageLoader] will compute a memory cache key.
         */
        fun key(key: MemoryCache.Key?) = apply {
            this.key = key
        }

        /**
         * Convenience function to create and set the [Listener].
         */
        inline fun listener(
            crossinline onStart: (request: ImageRequest) -> Unit = {},
            crossinline onCancel: (request: ImageRequest) -> Unit = {},
            crossinline onError: (request: ImageRequest, throwable: Throwable) -> Unit = { _, _ -> },
            crossinline onSuccess: (request: ImageRequest, metadata: Metadata) -> Unit = { _, _ -> }
        ) = listener(object : Listener {
            override fun onStart(request: ImageRequest) = onStart(request)
            override fun onCancel(request: ImageRequest) = onCancel(request)
            override fun onError(request: ImageRequest, throwable: Throwable) = onError(request, throwable)
            override fun onSuccess(request: ImageRequest, metadata: Metadata) = onSuccess(request, metadata)
        })

        /**
         * Set the [Listener].
         */
        fun listener(listener: Listener?) = apply {
            this.listener = listener
        }

        /**
         * Set the [CoroutineDispatcher] to launch the request.
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.dispatcher = dispatcher
        }

        /**
         * Set the list of [Transformation]s to be applied to this request.
         */
        fun transformations(vararg transformations: Transformation) = transformations(transformations.toList())

        /**
         * Set the list of [Transformation]s to be applied to this request.
         */
        fun transformations(transformations: List<Transformation>) = apply {
            this.transformations = transformations.toList()
        }

        /**
         * @see ImageLoader.Builder.bitmapConfig
         */
        fun bitmapConfig(config: Bitmap.Config) = apply {
            this.bitmapConfig = config
        }

        /**
         * Set the preferred [ColorSpace].
         */
        @RequiresApi(26)
        fun colorSpace(colorSpace: ColorSpace) = apply {
            this.colorSpace = colorSpace
        }

        /**
         * Set the requested width/height.
         */
        fun size(@Px size: Int) = size(size, size)

        /**
         * Set the requested width/height.
         */
        fun size(@Px width: Int, @Px height: Int) = size(PixelSize(width, height))

        /**
         * Set the requested width/height.
         */
        fun size(size: Size) = size(SizeResolver(size))

        /**
         * Set the [SizeResolver] to resolve the requested width/height.
         */
        fun size(resolver: SizeResolver) = apply {
            this.sizeResolver = resolver
        }

        /**
         * Set the scaling algorithm that will be used to fit/fill the image into the size provided by [sizeResolver].
         *
         * NOTE: If [scale] is not set, it is automatically computed for [ImageView] targets.
         */
        fun scale(scale: Scale) = apply {
            this.scale = scale
        }

        /**
         * Set the precision for the size of the loaded image.
         *
         * The default value is [Precision.AUTOMATIC], which uses the logic in [allowInexactSize]
         * to determine if output image's dimensions must match the input [size] and [scale] exactly.
         *
         * NOTE: If [size] is [OriginalSize], the returned image's size will always be equal to or greater than
         * the image's original size.
         *
         * @see Precision
         */
        fun precision(precision: Precision) = apply {
            this.precision = precision
        }

        /**
         * Use [fetcher] to handle fetching any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable fetcher in its [ComponentRegistry].
         */
        inline fun <reified T : Any> fetcher(fetcher: Fetcher<T>) = fetcher(fetcher, T::class.java)

        /**
         * Use [fetcher] to handle fetching any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable fetcher in its [ComponentRegistry].
         */
        @PublishedApi
        internal fun <T : Any> fetcher(fetcher: Fetcher<T>, type: Class<T>) = apply {
            this.fetcher = fetcher to type
        }

        /**
         * Use [decoder] to handle decoding any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable decoder in its [ComponentRegistry].
         */
        fun decoder(decoder: Decoder) = apply {
            this.decoder = decoder
        }

        /**
         * Enable/disable the use of [Bitmap.Config.HARDWARE] for this request.
         *
         * If false, any use of [Bitmap.Config.HARDWARE] will be treated as [Bitmap.Config.ARGB_8888].
         */
        fun allowHardware(enable: Boolean) = apply {
            this.allowHardware = enable
        }

        /**
         * @see ImageLoader.Builder.allowRgb565
         */
        fun allowRgb565(enable: Boolean) = apply {
            this.allowRgb565 = enable
        }

        /**
         * Enable/disable reading/writing from/to the memory cache.
         */
        fun memoryCachePolicy(policy: CachePolicy) = apply {
            this.memoryCachePolicy = policy
        }

        /**
         * Enable/disable reading/writing from/to the disk cache.
         */
        fun diskCachePolicy(policy: CachePolicy) = apply {
            this.diskCachePolicy = policy
        }

        /**
         * Enable/disable reading from the network.
         *
         * NOTE: Disabling writes has no effect.
         */
        fun networkCachePolicy(policy: CachePolicy) = apply {
            this.networkCachePolicy = policy
        }

        /**
         * Set the [Headers] for any network operations performed by this request.
         */
        fun headers(headers: Headers) = apply {
            this.headers = headers.newBuilder()
        }

        /**
         * Add a header for any network operations performed by this request.
         *
         * @see Headers.Builder.add
         */
        fun addHeader(name: String, value: String) = apply {
            this.headers = (this.headers ?: Headers.Builder()).add(name, value)
        }

        /**
         * Set a header for any network operations performed by this request.
         *
         * @see Headers.Builder.set
         */
        fun setHeader(name: String, value: String) = apply {
            this.headers = (this.headers ?: Headers.Builder()).set(name, value)
        }

        /**
         * Remove all network headers with the key [name].
         */
        fun removeHeader(name: String) = apply {
            this.headers = this.headers?.removeAll(name)
        }

        /**
         * Set the parameters for this request.
         */
        fun parameters(parameters: Parameters) = apply {
            this.parameters = parameters.newBuilder()
        }

        /**
         * Set a parameter for this request.
         *
         * @see Parameters.Builder.set
         */
        @JvmOverloads
        fun setParameter(key: String, value: Any?, cacheKey: String? = value?.toString()) = apply {
            this.parameters = (this.parameters ?: Parameters.Builder()).apply { set(key, value, cacheKey) }
        }

        /**
         * Remove a parameter from this request.
         *
         * @see Parameters.Builder.remove
         */
        fun removeParameter(key: String) = apply {
            this.parameters?.remove(key)
        }

        /**
         * Set the memory cache [key] whose value will be used as the placeholder drawable.
         *
         * If there is no value in the memory cache for [key], fall back to [placeholder].
         */
        fun placeholderKey(key: MemoryCache.Key?) = apply {
            this.placeholderKey = key
        }

        /**
         * Set the placeholder drawable to use when the request starts.
         */
        fun placeholder(@DrawableRes drawableResId: Int) = apply {
            this.placeholderResId = drawableResId
            this.placeholderDrawable = null
        }

        /**
         * Set the placeholder drawable to use when the request starts.
         */
        fun placeholder(drawable: Drawable?) = apply {
            this.placeholderDrawable = drawable
            this.placeholderResId = 0
        }

        /**
         * Set the error drawable to use if the request fails.
         */
        fun error(@DrawableRes drawableResId: Int) = apply {
            this.errorResId = drawableResId
            this.errorDrawable = null
        }

        /**
         * Set the error drawable to use if the request fails.
         */
        fun error(drawable: Drawable?) = apply {
            this.errorDrawable = drawable
            this.errorResId = 0
        }

        /**
         * Set the fallback drawable to use if [data] is null.
         */
        fun fallback(@DrawableRes drawableResId: Int) = apply {
            this.fallbackResId = drawableResId
            this.fallbackDrawable = null
        }

        /**
         * Set the fallback drawable to use if [data] is null.
         */
        fun fallback(drawable: Drawable?) = apply {
            this.fallbackDrawable = drawable
            this.fallbackResId = 0
        }

        /**
         * Convenience function to set [imageView] as the [Target].
         */
        fun target(imageView: ImageView) = target(ImageViewTarget(imageView))

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
         * @see ImageLoader.Builder.crossfade
         */
        fun crossfade(enable: Boolean) = crossfade(if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0)

        /**
         * @see ImageLoader.Builder.crossfade
         */
        fun crossfade(durationMillis: Int) =
            transition(if (durationMillis > 0) CrossfadeTransition(durationMillis) else Transition.NONE)

        /**
         * @see ImageLoader.Builder.transition
         */
        @ExperimentalCoilApi
        fun transition(transition: Transition) = apply {
            this.transition = transition
        }

        /**
         * Set the [Lifecycle] for this request.
         */
        fun lifecycle(owner: LifecycleOwner?) = lifecycle(owner?.lifecycle)

        /**
         * Set the [Lifecycle] for this request.
         *
         * Requests are queued while the lifecycle is not at least [Lifecycle.State.STARTED].
         * Requests are cancelled when the lifecycle reaches [Lifecycle.State.DESTROYED].
         *
         * If this is null or is not set the [ImageLoader] will attempt to find the lifecycle
         * for this request through its [context].
         */
        fun lifecycle(lifecycle: Lifecycle?) = apply {
            this.lifecycle = lifecycle
        }

        /**
         * Set the defaults for any unset request values.
         */
        fun defaults(defaults: DefaultRequestOptions) = apply {
            this.defaults = defaults
        }

        /**
         * Create a new [ImageRequest].
         */
        fun build(): ImageRequest {
            return ImageRequest(
                context,
                data ?: NullRequestData,
                target,
                listener,
                key,
                placeholderKey,
                colorSpace,
                fetcher,
                decoder,
                transformations,
                headers?.build().orEmpty(),
                parameters?.build().orEmpty(),
                lifecycle,
                sizeResolver,
                scale,
                dispatcher,
                transition,
                precision,
                bitmapConfig,
                allowHardware,
                allowRgb565,
                placeholderResId,
                placeholderDrawable,
                errorResId,
                errorDrawable,
                fallbackResId,
                fallbackDrawable,
                memoryCachePolicy,
                diskCachePolicy,
                networkCachePolicy,
                defaults
            )
        }
    }
}
