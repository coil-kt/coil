@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package coil

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.MainThread
import coil.request.ErrorResult
import coil.request.GetRequest
import coil.request.LoadRequest
import coil.request.Request
import coil.request.RequestDisposable
import coil.request.RequestResult
import coil.request.SuccessResult
import coil.target.Target

/**
 * A service class that loads images by executing [Request]s. Image loaders handle caching, data fetching,
 * image decoding, request management, bitmap pooling, memory management, and more.
 *
 * Image loaders are designed to be shareable and work best when you create a single instance and
 * share it throughout your app.
 *
 * It's recommended, though not required, to call [shutdown] when you've finished using an image loader.
 * This preemptively frees its memory and cleans up any observers.
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
     * Suspends and executes the [GetRequest]. Returns either [SuccessResult] or [ErrorResult] depending
     * on how the request completes.
     *
     * @param request The request to execute.
     * @return A [SuccessResult] if the request completes successfully. Else, returns an [ErrorResult].
     */
    suspend fun execute(request: GetRequest): RequestResult

    /**
     * Remove the value referenced by [key] from the memory cache.
     *
     * @param key The cache key to remove.
     */
    @MainThread
    fun invalidate(key: String)

    /**
     * Clear this image loader's memory cache and bitmap pool.
     */
    @MainThread
    fun clearMemory()

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
        replaceWith = ReplaceWith("this.execute(request)")
    )
    fun load(request: LoadRequest): RequestDisposable = execute(request)

    /** @see execute */
    @Deprecated(
        message = "Migrate to execute(request).",
        replaceWith = ReplaceWith(
            expression = "" +
                "when (val result = this.execute(request)) {\n" +
                "    is SuccessResult -> result.drawable\n" +
                "    is ErrorResult -> throw result.throwable\n" +
                "}",
            imports = ["coil.request.SuccessResult", "coil.request.ErrorResult"]
        )
    )
    suspend fun get(request: GetRequest): Drawable {
        return when (val result = execute(request)) {
            is SuccessResult -> result.drawable
            is ErrorResult -> throw result.throwable
        }
    }
}
