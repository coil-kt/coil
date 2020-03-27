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
 * A service class that fetches and decodes image data.
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
     * The default options for any [Request]s created by this image loader.
     */
    val defaults: DefaultRequestOptions

    /**
     * Launch an asynchronous operation that executes the [request] and sets the result on its [Target].
     *
     * If the request's target is null, this method preloads the image.
     *
     * @param request The request to execute.
     * @return A [RequestDisposable] which can be used to cancel or check the status of the request.
     */
    fun launch(request: LoadRequest): RequestDisposable

    /**
     * Executes the [request] and suspend until the operation is complete. Return the loaded [Drawable].
     *
     * @param request The request to execute.
     * @return The [Drawable] result.
     */
    suspend fun launch(request: GetRequest): Drawable

    /**
     * Completely clear this image loader's memory cache and bitmap pool.
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

    /** @see launch */
    @Deprecated(
        message = "Migrate to launch(request).",
        replaceWith = ReplaceWith("launch(request)"),
        level = DeprecationLevel.ERROR
    )
    fun load(request: LoadRequest): RequestDisposable = launch(request)

    /** @see launch */
    @Deprecated(
        message = "Migrate to launch(request).",
        replaceWith = ReplaceWith("launch(request)"),
        level = DeprecationLevel.ERROR
    )
    suspend fun get(request: GetRequest): Drawable = launch(request)
}
