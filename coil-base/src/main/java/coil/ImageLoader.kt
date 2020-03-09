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
 * Loads images using [load] and [get].
 */
interface ImageLoader {

    companion object {
        /** Alias to create an [ImageLoaderBuilder]. */
        @JvmStatic
        @JvmName("builder")
        inline fun Builder(context: Context) = ImageLoaderBuilder(context)

        /** Alias to create a new [ImageLoader] without configuration. */
        @JvmStatic
        @JvmName("create")
        inline operator fun invoke(context: Context) = ImageLoaderBuilder(context).build()

        /** Create a new [ImageLoader] instance. */
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
     * Start an asynchronous operation to load the [request]'s data into its [Target].
     *
     * If the request's target is null, this method preloads the image.
     *
     * @param request The request to execute.
     * @return A [RequestDisposable] which can be used to cancel or check the status of the request.
     */
    fun load(request: LoadRequest): RequestDisposable

    /**
     * Load the [request]'s data and suspend until the operation is complete. Return the loaded [Drawable].
     *
     * @param request The request to execute.
     * @return The [Drawable] result.
     */
    suspend fun get(request: GetRequest): Drawable

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
     * In progress [load] requests will be cancelled. In progress [get] requests will continue until complete.
     */
    @MainThread
    fun shutdown()
}
