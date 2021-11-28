@file:JvmName("ImageLoaders")
@file:Suppress("unused")

package coil

import android.content.Context
import androidx.annotation.WorkerThread
import coil.request.ImageRequest
import coil.request.ImageResult
import kotlinx.coroutines.runBlocking

/**
 * Create a new [ImageLoader] without configuration.
 */
@JvmName("create")
fun ImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context).build()
}

/**
 * Execute the [request] and block the current thread until it completes.
 *
 * @see ImageLoader.execute
 */
@WorkerThread
fun ImageLoader.executeBlocking(request: ImageRequest): ImageResult {
    return runBlocking { execute(request) }
}
