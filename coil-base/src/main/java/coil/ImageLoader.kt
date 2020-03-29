@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package coil

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.MainThread
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.Request
import coil.request.RequestDisposable
import coil.target.Target

/**
 * A service class that loads images by executing [Request]s. Image loaders handle caching, data fetching,
 * image decoding, request management, bitmap pooling, memory management, and more.
 *
 * Image loaders are designed to be shareable and work best when you create a single instance and
 * share it throughout your app.
 */
interface ImageLoader {

    companion object {
        /** Alias for [ImageLoaderBuilder]. */
        @JvmStatic
        @JvmName("builder")
        inline fun Builder(context: Context) = ImageLoaderBuilder(context)

        /** Create a new [ImageLoader] without configuration. */
        @JvmStatic
        @JvmName("create")
        inline operator fun invoke(context: Context) = ImageLoaderBuilder(context).build()

        /** Create a new [ImageLoader]. */
        @Deprecated(
            message = "Use ImageLoader.Builder to create new instances.",
            replaceWith = ReplaceWith("ImageLoader.Builder(context).apply(builder).build()")
        )
        inline operator fun invoke(
            context: Context,
            builder: ImageLoaderBuilder.() -> Unit = {}
        ): ImageLoader = ImageLoaderBuilder(context).apply(builder).build()
    }

    /**
     * The default options that are used to fill in unset [Request] values.
     */
    val defaults: DefaultRequestOptions

    /**
     * Launch an asynchronous operation that executes the [LoadRequest] and sets the result on its [Target].
     *
     * @param request The request to execute.
     * @return A [RequestDisposable] which can be used to cancel or check the status of the request.
     */
    fun execute(request: LoadRequest): RequestDisposable

    /**
     * Suspends and executes the [GetRequest]. Returns the loaded [Drawable] when complete.
     *
     * @param request The request to execute.
     * @return The [Drawable] result.
     */
    suspend fun execute(request: GetRequest): Drawable

    /**
     * Clear this image loader's memory cache and bitmap pool.
     */
    @MainThread
    fun clearMemory()

    /**
     * Remove a single item from memory cache.
     */
    fun invalidate(key: String)

    /**
     * Shutdown this image loader.
     *
     * All associated resources will be freed and any new requests will fail before starting.
     *
     * In progress [LoadRequest]s will be cancelled. In progress [GetRequest]s will continue until complete.
     */
    @MainThread
    fun shutdown()

    /** @see execute */
    @Deprecated(
        message = "Migrate to execute(request).",
        replaceWith = ReplaceWith("this.execute(request)"),
        level = DeprecationLevel.ERROR
    )
    fun load(request: LoadRequest): RequestDisposable = execute(request)

    /** @see execute */
    @Deprecated(
        message = "Migrate to execute(request).",
        replaceWith = ReplaceWith("this.execute(request)"),
        level = DeprecationLevel.ERROR
    )
    suspend fun get(request: GetRequest): Drawable = execute(request)
}
