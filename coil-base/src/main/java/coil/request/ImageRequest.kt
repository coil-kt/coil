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
import androidx.annotation.MainThread
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import coil.ComponentRegistry
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.decode.DataSource
import coil.decode.Decoder
import coil.drawable.CrossfadeDrawable
import coil.fetch.Fetcher
import coil.memory.RequestService
import coil.request.ImageRequest.Builder
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
import coil.util.getDrawableCompat
import coil.util.orEmpty
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.Headers
import okhttp3.HttpUrl
import java.io.File

class ImageRequest private constructor(
    val context: Context,

    /** @see Builder.data */
    val data: Any?,

    /** @see Builder.key */
    val key: String?,

    /** @see Builder.target */
    val target: Target?,

    /** @see Builder.listener */
    val listener: Listener?,

    /** @see Builder.lifecycle */
    val lifecycle: Lifecycle?,

    /** @see Builder.dispatcher */
    val dispatcher: CoroutineDispatcher?,

    /** @see Builder.transition */
    val transformations: List<Transformation>,

    /** @see Builder.transition */
    val transition: Transition?,

    /** @see Builder.bitmapConfig */
    val bitmapConfig: Bitmap.Config?,

    /** @see Builder.colorSpace */
    val colorSpace: ColorSpace?,

    /** @see Builder.size */
    val sizeResolver: SizeResolver?,

    /** @see Builder.scale */
    val scale: Scale?,

    /** @see Builder.precision */
    val precision: Precision?,

    /** @see Builder.fetcher */
    val fetcher: Pair<Class<*>, Fetcher<*>>?,

    /** @see Builder.decoder */
    val decoder: Decoder?,

    /** @see Builder.allowHardware */
    val allowHardware: Boolean?,

    /** @see Builder.allowRgb565 */
    val allowRgb565: Boolean?,

    /** @see Builder.memoryCachePolicy */
    val memoryCachePolicy: CachePolicy?,

    /** @see Builder.diskCachePolicy */
    val diskCachePolicy: CachePolicy?,

    /** @see Builder.networkCachePolicy */
    val networkCachePolicy: CachePolicy?,

    /** @see Builder.headers */
    val headers: Headers,

    /** @see Builder.parameters */
    val parameters: Parameters,

    private val placeholderResId: Int,
    private val placeholderDrawable: Drawable?,
    private val errorResId: Int,
    private val errorDrawable: Drawable?,
    private val fallbackResId: Int,
    private val fallbackDrawable: Drawable?
) {

    /** @see Builder.placeholder */
    val placeholder: Drawable? get() = getDrawableCompat(placeholderDrawable, placeholderResId)

    /** @see Builder.error */
    val error: Drawable? get() = getDrawableCompat(errorDrawable, errorResId)

    /** @see Builder.fallback */
    val fallback: Drawable? get() = getDrawableCompat(fallbackDrawable, fallbackResId)

    @JvmOverloads
    fun newBuilder(context: Context = this.context) = Builder(this, context)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ImageRequest &&
            context == other.context &&
            data == other.data &&
            key == other.key &&
            target == other.target &&
            listener == other.listener &&
            lifecycle == other.lifecycle &&
            dispatcher == other.dispatcher &&
            transformations == other.transformations &&
            transition == other.transition &&
            bitmapConfig == other.bitmapConfig &&
            colorSpace == other.colorSpace &&
            sizeResolver == other.sizeResolver &&
            scale == other.scale &&
            precision == other.precision &&
            fetcher == other.fetcher &&
            decoder == other.decoder &&
            allowHardware == other.allowHardware &&
            allowRgb565 == other.allowRgb565 &&
            memoryCachePolicy == other.memoryCachePolicy &&
            diskCachePolicy == other.diskCachePolicy &&
            networkCachePolicy == other.networkCachePolicy &&
            headers == other.headers &&
            parameters == other.parameters &&
            placeholderResId == other.placeholderResId &&
            placeholderDrawable == other.placeholderDrawable &&
            errorResId == other.errorResId &&
            errorDrawable == other.errorDrawable &&
            fallbackResId == other.fallbackResId &&
            fallbackDrawable == other.fallbackDrawable
    }

    override fun hashCode(): Int {
        var result = context.hashCode()
        result = 31 * result + (data?.hashCode() ?: 0)
        result = 31 * result + (key?.hashCode() ?: 0)
        result = 31 * result + (target?.hashCode() ?: 0)
        result = 31 * result + (listener?.hashCode() ?: 0)
        result = 31 * result + (lifecycle?.hashCode() ?: 0)
        result = 31 * result + (dispatcher?.hashCode() ?: 0)
        result = 31 * result + transformations.hashCode()
        result = 31 * result + (transition?.hashCode() ?: 0)
        result = 31 * result + (bitmapConfig?.hashCode() ?: 0)
        result = 31 * result + (colorSpace?.hashCode() ?: 0)
        result = 31 * result + (sizeResolver?.hashCode() ?: 0)
        result = 31 * result + (scale?.hashCode() ?: 0)
        result = 31 * result + (precision?.hashCode() ?: 0)
        result = 31 * result + (fetcher?.hashCode() ?: 0)
        result = 31 * result + (decoder?.hashCode() ?: 0)
        result = 31 * result + (allowHardware?.hashCode() ?: 0)
        result = 31 * result + (allowRgb565?.hashCode() ?: 0)
        result = 31 * result + (memoryCachePolicy?.hashCode() ?: 0)
        result = 31 * result + (diskCachePolicy?.hashCode() ?: 0)
        result = 31 * result + (networkCachePolicy?.hashCode() ?: 0)
        result = 31 * result + headers.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + placeholderResId
        result = 31 * result + (placeholderDrawable?.hashCode() ?: 0)
        result = 31 * result + errorResId
        result = 31 * result + (errorDrawable?.hashCode() ?: 0)
        result = 31 * result + fallbackResId
        result = 31 * result + (fallbackDrawable?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Request(context=$context, data=$data, key=$key, target=$target, listener=$listener, " +
            "lifecycle=$lifecycle, dispatcher=$dispatcher, transformations=$transformations, transition=$transition, " +
            "bitmapConfig=$bitmapConfig, colorSpace=$colorSpace, sizeResolver=$sizeResolver, scale=$scale, " +
            "precision=$precision, fetcher=$fetcher, decoder=$decoder, allowHardware=$allowHardware, " +
            "allowRgb565=$allowRgb565, memoryCachePolicy=$memoryCachePolicy, diskCachePolicy=$diskCachePolicy, " +
            "networkCachePolicy=$networkCachePolicy, headers=$headers, parameters=$parameters, " +
            "placeholderResId=$placeholderResId, placeholderDrawable=$placeholderDrawable, errorResId=$errorResId, " +
            "errorDrawable=$errorDrawable, fallbackResId=$fallbackResId, fallbackDrawable=$fallbackDrawable)"
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
        fun onSuccess(request: ImageRequest, source: DataSource) {}

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
        private var data: Any?
        private var key: String?

        private var target: Target?
        private var listener: Listener?

        private var lifecycle: Lifecycle?
        private var dispatcher: CoroutineDispatcher?
        private var transformations: List<Transformation>
        private var transition: Transition?

        private var bitmapConfig: Bitmap.Config?
        private var colorSpace: ColorSpace? = null

        private var sizeResolver: SizeResolver?
        private var scale: Scale?
        private var precision: Precision?

        private var fetcher: Pair<Class<*>, Fetcher<*>>?
        private var decoder: Decoder?

        private var allowHardware: Boolean?
        private var allowRgb565: Boolean?

        private var memoryCachePolicy: CachePolicy?
        private var diskCachePolicy: CachePolicy?
        private var networkCachePolicy: CachePolicy?

        private var headers: Headers.Builder?
        private var parameters: Parameters.Builder?

        @DrawableRes private var placeholderResId: Int
        private var placeholderDrawable: Drawable?
        @DrawableRes private var errorResId: Int
        private var errorDrawable: Drawable?
        @DrawableRes private var fallbackResId: Int
        private var fallbackDrawable: Drawable?

        constructor(context: Context) {
            this.context = context
            data = null
            key = null
            target = null
            listener = null
            lifecycle = null
            dispatcher = null
            transformations = emptyList()
            transition = null
            bitmapConfig = null
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
            placeholderResId = 0
            placeholderDrawable = null
            errorResId = 0
            errorDrawable = null
            fallbackResId = 0
            fallbackDrawable = null
        }

        constructor(request: ImageRequest, context: Context) {
            this.context = context
            data = request.data
            key = request.key
            target = request.target
            listener = request.listener
            lifecycle = request.lifecycle
            dispatcher = request.dispatcher
            transformations = request.transformations
            transition = request.transition
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
            placeholderResId = request.placeholderResId
            placeholderDrawable = request.placeholderDrawable
            errorResId = request.errorResId
            errorDrawable = request.errorDrawable
            fallbackResId = request.fallbackResId
            fallbackDrawable = request.fallbackDrawable
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
        fun data(data: Any?) = apply {
            this.data = data
        }

        /**
         * Set the cache key for this request.
         *
         * By default, the cache key is computed by the [Fetcher], any [Parameters], and any [Transformation]s.
         */
        fun key(key: String?) = apply {
            this.key = key
        }

        /**
         * Convenience function to create and set the [Listener].
         */
        inline fun listener(
            crossinline onStart: (request: ImageRequest) -> Unit = {},
            crossinline onCancel: (request: ImageRequest) -> Unit = {},
            crossinline onError: (request: ImageRequest, throwable: Throwable) -> Unit = { _, _ -> },
            crossinline onSuccess: (request: ImageRequest, source: DataSource) -> Unit = { _, _ -> }
        ) = listener(object : Listener {
            override fun onStart(request: ImageRequest) = onStart(request)
            override fun onCancel(request: ImageRequest) = onCancel(request)
            override fun onError(request: ImageRequest, throwable: Throwable) = onError(request, throwable)
            override fun onSuccess(request: ImageRequest, source: DataSource) = onSuccess(request, source)
        })

        /**
         * Set the [Listener].
         */
        fun listener(listener: Listener?) = apply {
            this.listener = listener
        }

        /**
         * Set the [CoroutineDispatcher] to run the fetching, decoding, and transforming work on.
         */
        fun dispatcher(dispatcher: CoroutineDispatcher) = apply {
            this.dispatcher = dispatcher
        }

        /**
         * Set the list of [Transformation]s to be applied to this request.
         */
        fun transformations(vararg transformations: Transformation) = apply {
            this.transformations = transformations.toList()
        }

        /**
         * Set the list of [Transformation]s to be applied to this request.
         */
        fun transformations(transformations: List<Transformation>) = apply {
            this.transformations = transformations.toList()
        }

        /**
         * @see ImageLoader.Builder.bitmapConfig
         */
        fun bitmapConfig(bitmapConfig: Bitmap.Config) = apply {
            this.bitmapConfig = bitmapConfig
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
        fun size(@Px size: Int) = apply {
            size(size, size)
        }

        /**
         * Set the requested width/height.
         */
        fun size(@Px width: Int, @Px height: Int) = apply {
            size(PixelSize(width, height))
        }

        /**
         * Set the requested width/height.
         */
        fun size(size: Size) = apply {
            this.sizeResolver = SizeResolver(size)
        }

        /**
         * Set the [SizeResolver] for this request. It will be used to determine the requested width/height for this request.
         *
         * If this isn't set, Coil will attempt to determine the size of the request using the logic in [RequestService.sizeResolver].
         */
        fun size(resolver: SizeResolver) = apply {
            this.sizeResolver = resolver
        }

        /**
         * Set the scaling algorithm that will be used to fit/fill the image into the dimensions provided by [sizeResolver].
         *
         * If this isn't set, Coil will attempt to determine the scale of the request using the logic in [RequestService.scale].
         *
         * NOTE: If [scale] is not set, it is automatically computed for [ImageView] targets.
         */
        fun scale(scale: Scale) = apply {
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
        fun precision(precision: Precision) = apply {
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
         * @see Builder.fetcher
         */
        @PublishedApi
        internal fun <R : Any> fetcher(type: Class<R>, fetcher: Fetcher<R>) = apply {
            this.fetcher = type to fetcher
        }

        /**
         * Set the [Decoder] to handle decoding any image data.
         *
         * If this isn't set, the [ImageLoader] will find an applicable [Decoder] that's registered in its [ComponentRegistry].
         *
         * NOTE: This skips calling [Decoder.handles] for [decoder].
         */
        fun decoder(decoder: Decoder) = apply {
            this.decoder = decoder
        }

        /**
         * Enable/disable the use of [Bitmap.Config.HARDWARE] for this request.
         *
         * If false, any use of [Bitmap.Config.HARDWARE] will be treated as [Bitmap.Config.ARGB_8888].
         *
         * This is useful for shared element transitions, which do not support hardware bitmaps.
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
         * @see ImageLoader.Builder.crossfade
         */
        fun crossfade(enable: Boolean) = apply {
            crossfade(if (enable) CrossfadeDrawable.DEFAULT_DURATION else 0)
        }

        /**
         * @see ImageLoader.Builder.crossfade
         */
        fun crossfade(durationMillis: Int) = apply {
            this.transition = if (durationMillis > 0) CrossfadeTransition(durationMillis) else Transition.NONE
        }

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
        fun lifecycle(owner: LifecycleOwner?) = apply {
            lifecycle(owner?.lifecycle)
        }

        /**
         * Set the [Lifecycle] for this request.
         *
         * Requests are queued while the lifecycle is not at least [Lifecycle.State.STARTED].
         * Requests are cancelled when the lifecycle reaches [Lifecycle.State.DESTROYED].
         *
         * If this isn't set, Coil will attempt to find the lifecycle for this request through its [context].
         */
        fun lifecycle(lifecycle: Lifecycle?) = apply {
            this.lifecycle = lifecycle
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
         * Create a new [ImageRequest].
         */
        fun build(): ImageRequest {
            return ImageRequest(
                context,
                data,
                key,
                target,
                listener,
                lifecycle,
                dispatcher,
                transformations,
                transition,
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
                placeholderResId,
                placeholderDrawable,
                errorResId,
                errorDrawable,
                fallbackResId,
                fallbackDrawable
            )
        }
    }
}
