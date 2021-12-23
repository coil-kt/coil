@file:Suppress("unused", "UNUSED_PARAMETER")

package coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.widget.ImageView
import android.widget.ImageView.ScaleType.CENTER
import android.widget.ImageView.ScaleType.MATRIX
import androidx.annotation.DrawableRes
import androidx.annotation.MainThread
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.Decoder
import coil.drawable.CrossfadeDrawable
import coil.fetch.Fetcher
import coil.memory.MemoryCache
import coil.request.ImageRequest.Builder
import coil.size.Dimension
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
import coil.util.DEFAULT_REQUEST_OPTIONS
import coil.util.allowInexactSize
import coil.util.getDrawableCompat
import coil.util.getLifecycle
import coil.util.orEmpty
import coil.util.scale
import coil.util.toImmutableList
import coil.util.toImmutableMap
import coil.util.unsupported
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.Headers
import okhttp3.HttpUrl
import java.io.File
import java.nio.ByteBuffer

/**
 * An immutable value object that represents a request for an image.
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

    /** @see Builder.memoryCacheKey */
    val memoryCacheKey: MemoryCache.Key?,

    /** @see Builder.diskCacheKey */
    val diskCacheKey: String?,

    /** @see Builder.colorSpace */
    val colorSpace: ColorSpace?,

    /** @see Builder.fetcherFactory */
    val fetcherFactory: Pair<Fetcher.Factory<*>, Class<*>>?,

    /** @see Builder.decoderFactory */
    val decoderFactory: Decoder.Factory?,

    /** @see Builder.transformations */
    val transformations: List<Transformation>,

    /** @see Builder.tag */
    val tags: Map<Class<*>, Any>,

    /** @see Builder.headers */
    val headers: Headers,

    /** @see Builder.parameters */
    val parameters: Parameters,

    /** @see Builder.lifecycle */
    val lifecycle: Lifecycle,

    /** @see Builder.sizeResolver */
    val sizeResolver: SizeResolver,

    /** @see Builder.scale */
    val scale: Scale,

    /** @see Builder.interceptorDispatcher */
    val interceptorDispatcher: CoroutineDispatcher,

    /** @see Builder.fetcherDispatcher */
    val fetcherDispatcher: CoroutineDispatcher,

    /** @see Builder.decoderDispatcher */
    val decoderDispatcher: CoroutineDispatcher,

    /** @see Builder.transformationDispatcher */
    val transformationDispatcher: CoroutineDispatcher,

    /** @see Builder.transitionFactory */
    val transitionFactory: Transition.Factory,

    /** @see Builder.precision */
    val precision: Precision,

    /** @see Builder.bitmapConfig */
    val bitmapConfig: Bitmap.Config,

    /** @see Builder.allowConversionToBitmap */
    val allowConversionToBitmap: Boolean,

    /** @see Builder.allowHardware */
    val allowHardware: Boolean,

    /** @see Builder.allowRgb565 */
    val allowRgb565: Boolean,

    /** @see Builder.premultipliedAlpha */
    val premultipliedAlpha: Boolean,

    /** @see Builder.memoryCachePolicy */
    val memoryCachePolicy: CachePolicy,

    /** @see Builder.diskCachePolicy */
    val diskCachePolicy: CachePolicy,

    /** @see Builder.networkCachePolicy */
    val networkCachePolicy: CachePolicy,

    /** @see Builder.placeholderMemoryCacheKey */
    val placeholderMemoryCacheKey: MemoryCache.Key?,

    private val placeholderResId: Int?,
    private val placeholderDrawable: Drawable?,
    private val errorResId: Int?,
    private val errorDrawable: Drawable?,
    private val fallbackResId: Int?,
    private val fallbackDrawable: Drawable?,

    /** The raw values set on [Builder]. */
    val defined: DefinedRequestOptions,

    /** The defaults used to fill unset values. */
    val defaults: DefaultRequestOptions,
) {

    /** @see Builder.placeholder */
    val placeholder: Drawable? get() =
        getDrawableCompat(placeholderDrawable, placeholderResId, defaults.placeholder)

    /** @see Builder.error */
    val error: Drawable? get() =
        getDrawableCompat(errorDrawable, errorResId, defaults.error)

    /** @see Builder.fallback */
    val fallback: Drawable? get() =
        getDrawableCompat(fallbackDrawable, fallbackResId, defaults.fallback)

    @JvmOverloads
    fun newBuilder(context: Context = this.context) = Builder(this, context)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ImageRequest &&
            context == other.context &&
            data == other.data &&
            target == other.target &&
            listener == other.listener &&
            memoryCacheKey == other.memoryCacheKey &&
            diskCacheKey == other.diskCacheKey &&
            (SDK_INT < 26 || colorSpace == other.colorSpace) &&
            fetcherFactory == other.fetcherFactory &&
            decoderFactory == other.decoderFactory &&
            transformations == other.transformations &&
            headers == other.headers &&
            parameters == other.parameters &&
            lifecycle == other.lifecycle &&
            sizeResolver == other.sizeResolver &&
            scale == other.scale &&
            interceptorDispatcher == other.interceptorDispatcher &&
            fetcherDispatcher == other.fetcherDispatcher &&
            decoderDispatcher == other.decoderDispatcher &&
            transformationDispatcher == other.transformationDispatcher &&
            transitionFactory == other.transitionFactory &&
            precision == other.precision &&
            bitmapConfig == other.bitmapConfig &&
            allowConversionToBitmap == other.allowConversionToBitmap &&
            allowHardware == other.allowHardware &&
            allowRgb565 == other.allowRgb565 &&
            premultipliedAlpha == other.premultipliedAlpha &&
            memoryCachePolicy == other.memoryCachePolicy &&
            diskCachePolicy == other.diskCachePolicy &&
            networkCachePolicy == other.networkCachePolicy &&
            placeholderMemoryCacheKey == other.placeholderMemoryCacheKey &&
            placeholderResId == other.placeholderResId &&
            placeholderDrawable == other.placeholderDrawable &&
            errorResId == other.errorResId &&
            errorDrawable == other.errorDrawable &&
            fallbackResId == other.fallbackResId &&
            fallbackDrawable == other.fallbackDrawable &&
            defined == other.defined &&
            defaults == other.defaults
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + (target?.hashCode() ?: 0)
        result = 31 * result + (listener?.hashCode() ?: 0)
        result = 31 * result + (memoryCacheKey?.hashCode() ?: 0)
        result = 31 * result + (diskCacheKey?.hashCode() ?: 0)
        result = 31 * result + if (SDK_INT < 26) 0 else (colorSpace?.hashCode() ?: 0)
        result = 31 * result + (fetcherFactory?.hashCode() ?: 0)
        result = 31 * result + (decoderFactory?.hashCode() ?: 0)
        result = 31 * result + transformations.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + lifecycle.hashCode()
        result = 31 * result + sizeResolver.hashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + interceptorDispatcher.hashCode()
        result = 31 * result + fetcherDispatcher.hashCode()
        result = 31 * result + decoderDispatcher.hashCode()
        result = 31 * result + transformationDispatcher.hashCode()
        result = 31 * result + transitionFactory.hashCode()
        result = 31 * result + precision.hashCode()
        result = 31 * result + bitmapConfig.hashCode()
        result = 31 * result + allowConversionToBitmap.hashCode()
        result = 31 * result + allowHardware.hashCode()
        result = 31 * result + allowRgb565.hashCode()
        result = 31 * result + premultipliedAlpha.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
        result = 31 * result + (placeholderMemoryCacheKey?.hashCode() ?: 0)
        result = 31 * result + (placeholderResId ?: 0)
        result = 31 * result + (placeholderDrawable?.hashCode() ?: 0)
        result = 31 * result + (errorResId ?: 0)
        result = 31 * result + (errorDrawable?.hashCode() ?: 0)
        result = 31 * result + (fallbackResId ?: 0)
        result = 31 * result + (fallbackDrawable?.hashCode() ?: 0)
        result = 31 * result + defined.hashCode()
        result = 31 * result + defaults.hashCode()
        return result
    }

    /**
     * A set of callbacks for an [ImageRequest].
     */
    interface Listener {

        /**
         * Called immediately after [Target.onStart].
         */
        @MainThread
        fun onStart(request: ImageRequest) {}

        /**
         * Called if the request is cancelled.
         */
        @MainThread
        fun onCancel(request: ImageRequest) {}

        /**
         * Called if an error occurs while executing the request.
         */
        @MainThread
        fun onError(request: ImageRequest, result: ErrorResult) {}

        /**
         * Called if the request completes successfully.
         */
        @MainThread
        fun onSuccess(request: ImageRequest, result: SuccessResult) {}
    }

    class Builder {

        private val context: Context
        private var defaults: DefaultRequestOptions
        private var data: Any?

        private var target: Target?
        private var listener: Listener?
        private var memoryCacheKey: MemoryCache.Key?
        private var diskCacheKey: String?
        private var colorSpace: ColorSpace? = null
        private var fetcherFactory: Pair<Fetcher.Factory<*>, Class<*>>?
        private var decoderFactory: Decoder.Factory?
        private var transformations: List<Transformation>

        private var headers: Headers.Builder?
        private var tags: MutableMap<Class<*>, Any>?
        private var parameters: Parameters.Builder?

        private var lifecycle: Lifecycle?
        private var sizeResolver: SizeResolver?
        private var scale: Scale?

        private var interceptorDispatcher: CoroutineDispatcher?
        private var fetcherDispatcher: CoroutineDispatcher?
        private var decoderDispatcher: CoroutineDispatcher?
        private var transformationDispatcher: CoroutineDispatcher?

        private var transitionFactory: Transition.Factory?
        private var precision: Precision?
        private var bitmapConfig: Bitmap.Config?
        private var allowHardware: Boolean?
        private var allowRgb565: Boolean?
        private var premultipliedAlpha: Boolean
        private var allowConversionToBitmap: Boolean
        private var memoryCachePolicy: CachePolicy?
        private var diskCachePolicy: CachePolicy?
        private var networkCachePolicy: CachePolicy?

        private var placeholderMemoryCacheKey: MemoryCache.Key?
        @DrawableRes private var placeholderResId: Int?
        private var placeholderDrawable: Drawable?
        @DrawableRes private var errorResId: Int?
        private var errorDrawable: Drawable?
        @DrawableRes private var fallbackResId: Int?
        private var fallbackDrawable: Drawable?

        private var resolvedLifecycle: Lifecycle?
        private var resolvedSizeResolver: SizeResolver?
        private var resolvedScale: Scale?

        constructor(context: Context) {
            this.context = context
            defaults = DEFAULT_REQUEST_OPTIONS
            data = null
            target = null
            listener = null
            memoryCacheKey = null
            diskCacheKey = null
            if (SDK_INT >= 26) colorSpace = null
            fetcherFactory = null
            decoderFactory = null
            transformations = emptyList()
            headers = null
            tags = null
            parameters = null
            lifecycle = null
            sizeResolver = null
            scale = null
            interceptorDispatcher = null
            fetcherDispatcher = null
            decoderDispatcher = null
            transformationDispatcher = null
            transitionFactory = null
            precision = null
            bitmapConfig = null
            allowHardware = null
            allowRgb565 = null
            premultipliedAlpha = true
            allowConversionToBitmap = true
            memoryCachePolicy = null
            diskCachePolicy = null
            networkCachePolicy = null
            placeholderMemoryCacheKey = null
            placeholderResId = null
            placeholderDrawable = null
            errorResId = null
            errorDrawable = null
            fallbackResId = null
            fallbackDrawable = null
            resolvedLifecycle = null
            resolvedSizeResolver = null
            resolvedScale = null
        }

        @JvmOverloads
        constructor(request: ImageRequest, context: Context = request.context) {
            this.context = context
            defaults = request.defaults
            data = request.data
            target = request.target
            listener = request.listener
            memoryCacheKey = request.memoryCacheKey
            diskCacheKey = request.diskCacheKey
            if (SDK_INT >= 26) colorSpace = request.colorSpace
            fetcherFactory = request.fetcherFactory
            decoderFactory = request.decoderFactory
            transformations = request.transformations
            headers = request.headers.newBuilder()
            tags = request.tags.toMutableMap()
            parameters = request.parameters.newBuilder()
            lifecycle = request.defined.lifecycle
            sizeResolver = request.defined.sizeResolver
            scale = request.defined.scale
            interceptorDispatcher = request.defined.interceptorDispatcher
            fetcherDispatcher = request.defined.fetcherDispatcher
            decoderDispatcher = request.defined.decoderDispatcher
            transformationDispatcher = request.defined.transformationDispatcher
            transitionFactory = request.defined.transitionFactory
            precision = request.defined.precision
            bitmapConfig = request.defined.bitmapConfig
            allowHardware = request.defined.allowHardware
            allowRgb565 = request.defined.allowRgb565
            premultipliedAlpha = request.premultipliedAlpha
            allowConversionToBitmap = request.allowConversionToBitmap
            memoryCachePolicy = request.defined.memoryCachePolicy
            diskCachePolicy = request.defined.diskCachePolicy
            networkCachePolicy = request.defined.networkCachePolicy
            placeholderMemoryCacheKey = request.placeholderMemoryCacheKey
            placeholderResId = request.placeholderResId
            placeholderDrawable = request.placeholderDrawable
            errorResId = request.errorResId
            errorDrawable = request.errorDrawable
            fallbackResId = request.fallbackResId
            fallbackDrawable = request.fallbackDrawable

            // If the context changes, recompute the resolved values.
            if (request.context === context) {
                resolvedLifecycle = request.lifecycle
                resolvedSizeResolver = request.sizeResolver
                resolvedScale = request.scale
            } else {
                resolvedLifecycle = null
                resolvedSizeResolver = null
                resolvedScale = null
            }
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
         * - [ByteBuffer]
         */
        fun data(data: Any?) = apply {
            this.data = data
        }

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set, the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: String?) = memoryCacheKey(key?.let { MemoryCache.Key(it) })

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set, the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: MemoryCache.Key?) = apply {
            this.memoryCacheKey = key
        }

        /**
         * Set the disk cache key for this request.
         *
         * If this is null or is not set, the [ImageLoader] will compute a disk cache key.
         */
        fun diskCacheKey(key: String?) = apply {
            this.diskCacheKey = key
        }

        /**
         * Convenience function to create and set the [Listener].
         */
        inline fun listener(
            crossinline onStart: (request: ImageRequest) -> Unit = {},
            crossinline onCancel: (request: ImageRequest) -> Unit = {},
            crossinline onError: (request: ImageRequest, result: ErrorResult) -> Unit = { _, _ -> },
            crossinline onSuccess: (request: ImageRequest, result: SuccessResult) -> Unit = { _, _ -> }
        ) = listener(object : Listener {
            override fun onStart(request: ImageRequest) = onStart(request)
            override fun onCancel(request: ImageRequest) = onCancel(request)
            override fun onError(request: ImageRequest, result: ErrorResult) = onError(request, result)
            override fun onSuccess(request: ImageRequest, result: SuccessResult) = onSuccess(request, result)
        })

        /**
         * Set the [Listener].
         */
        fun listener(listener: Listener?) = apply {
            this.listener = listener
        }

        /**
         * @see ImageLoader.Builder.dispatcher
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.fetcherDispatcher = dispatcher
            this.decoderDispatcher = dispatcher
            this.transformationDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.interceptorDispatcher
         */
        fun interceptorDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.interceptorDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.fetcherDispatcher
         */
        fun fetcherDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.fetcherDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.decoderDispatcher
         */
        fun decoderDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.decoderDispatcher = dispatcher
        }

        /**
         * @see ImageLoader.Builder.transformationDispatcher
         */
        fun transformationDispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.transformationDispatcher = dispatcher
        }

        /**
         * Set the list of [Transformation]s to be applied to this request.
         */
        fun transformations(vararg transformations: Transformation) =
            transformations(transformations.toList())

        /**
         * Set the list of [Transformation]s to be applied to this request.
         */
        fun transformations(transformations: List<Transformation>) = apply {
            this.transformations = transformations.toImmutableList()
        }

        /**
         * @see ImageLoader.Builder.bitmapConfig
         */
        fun bitmapConfig(config: Bitmap.Config) = apply {
            this.bitmapConfig = config
        }

        /**
         * Set the preferred [ColorSpace].
         *
         * This is not guaranteed and a different color space may be used in some situations.
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
        fun size(@Px width: Int, @Px height: Int) = size(Size(width, height))

        /**
         * Set the requested width/height.
         */
        fun size(width: Dimension, height: Dimension) = size(Size(width, height))

        /**
         * Set the requested width/height.
         */
        fun size(size: Size) = size(SizeResolver(size))

        /**
         * Set the [SizeResolver] to resolve the requested width/height.
         */
        fun size(resolver: SizeResolver) = apply {
            this.sizeResolver = resolver
            resetResolvedValues()
        }

        /**
         * Set the scaling algorithm that will be used to fit/fill the image into the size provided
         * by [sizeResolver].
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
         * NOTE: If [size] is [Size.ORIGINAL], the returned image's size will always be equal to or
         * greater than the image's original size.
         *
         * @see Precision
         */
        fun precision(precision: Precision) = apply {
            this.precision = precision
        }

        /**
         * Use [factory] to handle fetching any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable fetcher in its
         * [ComponentRegistry].
         */
        inline fun <reified T : Any> fetcherFactory(factory: Fetcher.Factory<T>) =
            fetcherFactory(factory, T::class.java)

        /**
         * Use [factory] to handle fetching any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable fetcher in its
         * [ComponentRegistry].
         */
        fun <T : Any> fetcherFactory(factory: Fetcher.Factory<T>, type: Class<T>) = apply {
            this.fetcherFactory = factory to type
        }

        /**
         * Use [factory] to handle decoding any image data.
         *
         * If this is null or is not set the [ImageLoader] will find an applicable decoder in its
         * [ComponentRegistry].
         */
        fun decoderFactory(factory: Decoder.Factory) = apply {
            this.decoderFactory = factory
        }

        /**
         * Allow converting the result drawable to a bitmap to apply any [transformations].
         *
         * If false and the result drawable is not a [BitmapDrawable] any [transformations] will
         * be ignored.
         */
        fun allowConversionToBitmap(enable: Boolean) = apply {
            this.allowConversionToBitmap = enable
        }

        /**
         * @see ImageLoader.Builder.allowHardware
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
         * Enable/disable pre-multiplication of the color (RGB) channels of the decoded image by
         * the alpha channel.
         *
         * The default behavior is to enable pre-multiplication but in some environments it can be
         * necessary to disable this feature to leave the source pixels unmodified.
         */
        fun premultipliedAlpha(enable: Boolean) = apply {
            this.premultipliedAlpha = enable
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
         *
         */
        inline fun <reified T> tag(tag: T?) = tag(T::class.java, tag)

        /**
         *
         */
        fun <T> tag(type: Class<in T>, tag: T?) = apply {
            if (tag == null) {
                this.tags?.remove(type)
            } else {
                this.tags = (this.tags ?: mutableMapOf()).apply { put(type, tag) }
            }
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
        fun placeholderMemoryCacheKey(key: String?) =
            placeholderMemoryCacheKey(key?.let { MemoryCache.Key(it) })

        /**
         * Set the memory cache [key] whose value will be used as the placeholder drawable.
         *
         * If there is no value in the memory cache for [key], fall back to [placeholder].
         */
        fun placeholderMemoryCacheKey(key: MemoryCache.Key?) = apply {
            this.placeholderMemoryCacheKey = key
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
         * Set the [Target].
         */
        fun target(target: Target?) = apply {
            this.target = target
            resetResolvedValues()
        }

        /**
         * @see ImageLoader.Builder.crossfade
         */
        fun crossfade(enable: Boolean) =
            crossfade(if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0)

        /**
         * @see ImageLoader.Builder.crossfade
         */
        fun crossfade(durationMillis: Int) = apply {
            val factory = if (durationMillis > 0) {
                CrossfadeTransition.Factory(durationMillis)
            } else {
                Transition.Factory.NONE
            }
            transitionFactory(factory)
        }

        /**
         * @see ImageLoader.Builder.transitionFactory
         */
        fun transitionFactory(transition: Transition.Factory) = apply {
            this.transitionFactory = transition
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
            resetResolvedScale()
        }

        /**
         * Create a new [ImageRequest].
         */
        fun build(): ImageRequest {
            return ImageRequest(
                context = context,
                data = data ?: NullRequestData,
                target = target,
                listener = listener,
                memoryCacheKey = memoryCacheKey,
                diskCacheKey = diskCacheKey,
                colorSpace = colorSpace,
                fetcherFactory = fetcherFactory,
                decoderFactory = decoderFactory,
                transformations = transformations,
                headers = headers?.build().orEmpty(),
                tags = tags?.toImmutableMap().orEmpty(),
                parameters = parameters?.build().orEmpty(),
                lifecycle = lifecycle ?: resolvedLifecycle ?: resolveLifecycle(),
                sizeResolver = sizeResolver ?: resolvedSizeResolver ?: resolveSizeResolver(),
                scale = scale ?: resolvedScale ?: resolveScale(),
                interceptorDispatcher = interceptorDispatcher ?: defaults.interceptorDispatcher,
                fetcherDispatcher = fetcherDispatcher ?: defaults.fetcherDispatcher,
                decoderDispatcher = decoderDispatcher ?: defaults.decoderDispatcher,
                transformationDispatcher = transformationDispatcher ?: defaults.transformationDispatcher,
                transitionFactory = transitionFactory ?: defaults.transitionFactory,
                precision = precision ?: defaults.precision,
                bitmapConfig = bitmapConfig ?: defaults.bitmapConfig,
                allowConversionToBitmap = allowConversionToBitmap,
                allowHardware = allowHardware ?: defaults.allowHardware,
                allowRgb565 = allowRgb565 ?: defaults.allowRgb565,
                premultipliedAlpha = premultipliedAlpha,
                memoryCachePolicy = memoryCachePolicy ?: defaults.memoryCachePolicy,
                diskCachePolicy = diskCachePolicy ?: defaults.diskCachePolicy,
                networkCachePolicy = networkCachePolicy ?: defaults.networkCachePolicy,
                placeholderMemoryCacheKey = placeholderMemoryCacheKey,
                placeholderResId = placeholderResId,
                placeholderDrawable = placeholderDrawable,
                errorResId = errorResId,
                errorDrawable = errorDrawable,
                fallbackResId = fallbackResId,
                fallbackDrawable = fallbackDrawable,
                defined = DefinedRequestOptions(lifecycle, sizeResolver, scale, interceptorDispatcher,
                    fetcherDispatcher, decoderDispatcher, transformationDispatcher, transitionFactory,
                    precision, bitmapConfig, allowHardware, allowRgb565, memoryCachePolicy,
                    diskCachePolicy, networkCachePolicy),
                defaults = defaults,
            )
        }

        /** Ensure these values will be recomputed when [build] is called. */
        private fun resetResolvedValues() {
            resolvedLifecycle = null
            resolvedSizeResolver = null
            resolvedScale = null
        }

        /** Ensure the scale will be recomputed when [build] is called. */
        private fun resetResolvedScale() {
            resolvedScale = null
        }

        private fun resolveLifecycle(): Lifecycle {
            val target = target
            val context = if (target is ViewTarget<*>) target.view.context else context
            return context.getLifecycle() ?: GlobalLifecycle
        }

        private fun resolveSizeResolver(): SizeResolver {
            val target = target
            if (target is ViewTarget<*>) {
                val view = target.view
                // CENTER and MATRIX scale types should be decoded at the image's original size.
                if (view !is ImageView || view.scaleType.let { it != CENTER && it != MATRIX }) {
                    return ViewSizeResolver(view)
                }
            }
            return SizeResolver(Size.ORIGINAL)
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

        @Deprecated(
            message = "Migrate to 'fetcherFactory'.",
            replaceWith = ReplaceWith("fetcherFactory<Any> { _, _, _ -> fetcher }"),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun fetcher(fetcher: Fetcher): Builder = unsupported()

        @Deprecated(
            message = "Migrate to 'decoderFactory'.",
            replaceWith = ReplaceWith("decoderFactory { _, _, _ -> decoder }"),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun decoder(decoder: Decoder): Builder = unsupported()

        @Deprecated(
            message = "Migrate to 'transitionFactory'.",
            replaceWith = ReplaceWith("transitionFactory { _, _ -> transition }"),
            level = DeprecationLevel.ERROR // Temporary migration aid.
        )
        fun transition(transition: Transition): Builder = unsupported()
    }
}
