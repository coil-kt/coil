@file:JvmName("ImageLoaders")
@file:Suppress("unused")

package coil

import androidx.annotation.WorkerThread
import coil.request.ImageRequest
import coil.request.ImageResult
import kotlinx.coroutines.runBlocking

/**
 * Execute the [request] and block the current thread until it completes.
 *
 * @see ImageLoader.execute
 */
@WorkerThread
fun ImageLoader.executeBlocking(request: ImageRequest): ImageResult {
    return runBlocking { execute(request) }
}
