@file:Suppress("unused")

package coil.request

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
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
import coil.util.allowInexactSize
import coil.util.getDrawableCompat
import coil.util.getLifecycle
import coil.util.orEmpty
import coil.util.scale
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.Headers
import okhttp3.HttpUrl
import java.io.File

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

    /** @see Builder.placeholderMemoryCacheKey */
    val placeholderMemoryCacheKey: MemoryCache.Key?,

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

    /** @see Builder.lifecycle */
    val lifecycle: Lifecycle,

    /** @see Builder.sizeResolver */
    val sizeResolver: SizeResolver,

    /** @see Builder.scale */
    val scale: Scale,

    /** @see Builder.dispatcher */
    val dispatcher: CoroutineDispatcher,

    /** @see Builder.transition */
    val transition: Transition,

    /** @see Builder.precision */
    val precision: Precision,

    /** @see Builder.bitmapConfig */
    val bitmapConfig: Bitmap.Config,

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

    private val placeholderResId: Int?,
    private val placeholderDrawable: Drawable?,
    private val errorResId: Int?,
    private val errorDrawable: Drawable?,
    private val fallbackResId: Int?,
    private val fallbackDrawable: Drawable?,

    /** The raw values set on [Builder]. */
    val defined: DefinedRequestOptions,

    /** The defaults used to fill unset values. */
    val defaults: DefaultRequestOptions
) {

    /** @see Builder.placeholder */
    val placeholder: Drawable? get() = getDrawableCompat(placeholderDrawable, placeholderResId, defaults.placeholder)

    /** @see Builder.error */
    val error: Drawable? get() = getDrawableCompat(errorDrawable, errorResId, defaults.error)

    /** @see Builder.fallback */
    val fallback: Drawable? get() = getDrawableCompat(fallbackDrawable, fallbackResId, defaults.fallback)

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
            placeholderMemoryCacheKey == other.placeholderMemoryCacheKey &&
            colorSpace == other.colorSpace &&
            fetcher == other.fetcher &&
            decoder == other.decoder &&
            transformations == other.transformations &&
            headers == other.headers &&
            parameters == other.parameters &&
            lifecycle == other.lifecycle &&
            sizeResolver == other.sizeResolver &&
            scale == other.scale &&
            dispatcher == other.dispatcher &&
            transition == other.transition &&
            precision == other.precision &&
            bitmapConfig == other.bitmapConfig &&
            allowHardware == other.allowHardware &&
            allowRgb565 == other.allowRgb565 &&
            premultipliedAlpha == other.premultipliedAlpha &&
            memoryCachePolicy == other.memoryCachePolicy &&
            diskCachePolicy == other.diskCachePolicy &&
            networkCachePolicy == other.networkCachePolicy &&
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
        result = 31 * result + (placeholderMemoryCacheKey?.hashCode() ?: 0)
        result = 31 * result + (colorSpace?.hashCode() ?: 0)
        result = 31 * result + (fetcher?.hashCode() ?: 0)
        result = 31 * result + (decoder?.hashCode() ?: 0)
        result = 31 * result + transformations.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + lifecycle.hashCode()
        result = 31 * result + sizeResolver.hashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + dispatcher.hashCode()
        result = 31 * result + transition.hashCode()
        result = 31 * result + precision.hashCode()
        result = 31 * result + bitmapConfig.hashCode()
        result = 31 * result + allowHardware.hashCode()
        result = 31 * result + allowRgb565.hashCode()
        result = 31 * result + premultipliedAlpha.hashCode()
        result = 31 * result + memoryCachePolicy.hashCode()
        result = 31 * result + diskCachePolicy.hashCode()
        result = 31 * result + networkCachePolicy.hashCode()
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

    override fun toString(): String {
        return "ImageRequest(context=$context, data=$data, target=$target, listener=$listener, " +
            "memoryCacheKey=$memoryCacheKey, placeholderMemoryCacheKey=$placeholderMemoryCacheKey, " +
            "colorSpace=$colorSpace, fetcher=$fetcher, decoder=$decoder, transformations=$transformations, " +
            "headers=$headers, parameters=$parameters, lifecycle=$lifecycle, sizeResolver=$sizeResolver, " +
            "scale=$scale, dispatcher=$dispatcher, transition=$transition, precision=$precision, " +
            "bitmapConfig=$bitmapConfig, allowHardware=$allowHardware, allowRgb565=$allowRgb565, " +
            "premultipliedAlpha=$premultipliedAlpha, memoryCachePolicy=$memoryCachePolicy, " +
            "diskCachePolicy=$diskCachePolicy, networkCachePolicy=$networkCachePolicy, " +
            "placeholderResId=$placeholderResId, placeholderDrawable=$placeholderDrawable, errorResId=$errorResId, " +
            "errorDrawable=$errorDrawable, fallbackResId=$fallbackResId, fallbackDrawable=$fallbackDrawable, " +
            "defined=$defined, defaults=$defaults)"
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
        fun onError(request: ImageRequest, throwable: Throwable) {}

        /**
         * Called if the request completes successfully.
         */
        @MainThread
        fun onSuccess(request: ImageRequest, metadata: ImageResult.Metadata) {}
    }

    class Builder {

        private val context: Context
        private var defaults: DefaultRequestOptions
        private var data: Any?

        private var target: Target?
        private var listener: Listener?
        private var memoryCacheKey: MemoryCache.Key?
        private var placeholderMemoryCacheKey: MemoryCache.Key?
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
        private var premultipliedAlpha: Boolean
        private var memoryCachePolicy: CachePolicy?
        private var diskCachePolicy: CachePolicy?
        private var networkCachePolicy: CachePolicy?

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
            defaults = DefaultRequestOptions.INSTANCE
            data = null
            target = null
            listener = null
            memoryCacheKey = null
            placeholderMemoryCacheKey = null
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
            premultipliedAlpha = true
            memoryCachePolicy = null
            diskCachePolicy = null
            networkCachePolicy = null
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
            placeholderMemoryCacheKey = request.placeholderMemoryCacheKey
            if (SDK_INT >= 26) colorSpace = request.colorSpace
            fetcher = request.fetcher
            decoder = request.decoder
            transformations = request.transformations
            headers = request.headers.newBuilder()
            parameters = request.parameters.newBuilder()
            lifecycle = request.defined.lifecycle
            sizeResolver = request.defined.sizeResolver
            scale = request.defined.scale
            dispatcher = request.defined.dispatcher
            transition = request.defined.transition
            precision = request.defined.precision
            bitmapConfig = request.defined.bitmapConfig
            allowHardware = request.defined.allowHardware
            allowRgb565 = request.defined.allowRgb565
            premultipliedAlpha = request.premultipliedAlpha
            memoryCachePolicy = request.defined.memoryCachePolicy
            diskCachePolicy = request.defined.diskCachePolicy
            networkCachePolicy = request.defined.networkCachePolicy
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
         */
        fun data(data: Any?) = apply {
            this.data = data
        }

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: String?) = memoryCacheKey(key?.let { MemoryCache.Key(it) })

        /**
         * Set the memory cache key for this request.
         *
         * If this is null or is not set the [ImageLoader] will compute a memory cache key.
         */
        fun memoryCacheKey(key: MemoryCache.Key?) = apply {
            this.memoryCacheKey = key
        }

        /**
         * Convenience function to create and set the [Listener].
         */
        inline fun listener(
            crossinline onStart: (request: ImageRequest) -> Unit = {},
            crossinline onCancel: (request: ImageRequest) -> Unit = {},
            crossinline onError: (request: ImageRequest, throwable: Throwable) -> Unit = { _, _ -> },
            crossinline onSuccess: (request: ImageRequest, metadata: ImageResult.Metadata) -> Unit = { _, _ -> }
        ) = listener(object : Listener {
            override fun onStart(request: ImageRequest) = onStart(request)
            override fun onCancel(request: ImageRequest) = onCancel(request)
            override fun onError(request: ImageRequest, throwable: Throwable) = onError(request, throwable)
            override fun onSuccess(request: ImageRequest, metadata: ImageResult.Metadata) = onSuccess(request, metadata)
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
            resetResolvedValues()
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
         * Enable/disable pre-multiplication of the color (RGB) channels of the decoded image by the alpha channel.
         *
         * The default behavior is to enable pre-multiplication but in some environments it can be necessary
         * to disable this feature to leave the source pixels unmodified.
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
        fun placeholderMemoryCacheKey(key: String?) = placeholderMemoryCacheKey(key?.let { MemoryCache.Key(it) })

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
                placeholderMemoryCacheKey = placeholderMemoryCacheKey,
                colorSpace = colorSpace,
                fetcher = fetcher,
                decoder = decoder,
                transformations = transformations,
                headers = headers?.build().orEmpty(),
                parameters = parameters?.build().orEmpty(),
                lifecycle = lifecycle ?: resolvedLifecycle ?: resolveLifecycle(),
                sizeResolver = sizeResolver ?: resolvedSizeResolver ?: resolveSizeResolver(),
                scale = scale ?: resolvedScale ?: resolveScale(),
                dispatcher = dispatcher ?: defaults.dispatcher,
                transition = transition ?: defaults.transition,
                precision = precision ?: defaults.precision,
                bitmapConfig = bitmapConfig ?: defaults.bitmapConfig,
                allowHardware = allowHardware ?: defaults.allowHardware,
                allowRgb565 = allowRgb565 ?: defaults.allowRgb565,
                premultipliedAlpha = premultipliedAlpha,
                memoryCachePolicy = memoryCachePolicy ?: defaults.memoryCachePolicy,
                diskCachePolicy = diskCachePolicy ?: defaults.diskCachePolicy,
                networkCachePolicy = networkCachePolicy ?: defaults.networkCachePolicy,
                defined = DefinedRequestOptions(lifecycle, sizeResolver, scale, dispatcher, transition, precision,
                    bitmapConfig, allowHardware, allowRgb565, memoryCachePolicy, diskCachePolicy, networkCachePolicy),
                defaults = defaults,
                placeholderResId = placeholderResId,
                placeholderDrawable = placeholderDrawable,
                errorResId = errorResId,
                errorDrawable = errorDrawable,
                fallbackResId = fallbackResId,
                fallbackDrawable = fallbackDrawable
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
            return if (target is ViewTarget<*>) {
                val view = target.view
                if (view is ImageView && view.scaleType.let { it == CENTER || it == MATRIX }) {
                    SizeResolver(OriginalSize)
                } else {
                    ViewSizeResolver(view)
                }
            } else {
                DisplaySizeResolver(context)
            }
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

            return Scale.FILL
        }
    }
}
