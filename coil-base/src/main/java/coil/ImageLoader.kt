package coil

import android.content.Context
import android.graphics.drawable.Drawable
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
        /**
         * Create a new [ImageLoader] instance.
         *
         * Example:
         * ```
         * val loader = ImageLoader(context) {
         *     availableMemoryPercentage(0.5)
         *     crossfade(true)
         * }
         * ```
         */
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
    fun clearMemory()

    /**
     * Remove a single item from memory cache.
     */
    fun clearMemory(key: String)

    /**
     * Shutdown this image loader.
     *
     * All associated resources will be freed and any new requests will fail before starting.
     *
     * In-flight [load] requests will be cancelled. In-flight [get] requests will continue until complete.
     */
    fun shutdown()
}
