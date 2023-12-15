package coil3

import coil3.annotation.WorkerThread
import coil3.request.ImageRequest
import coil3.request.ImageResult
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
