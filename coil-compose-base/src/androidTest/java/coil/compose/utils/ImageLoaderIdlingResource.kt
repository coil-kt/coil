package coil.compose.utils

import androidx.compose.ui.test.IdlingResource
import coil.EventListener
import coil.request.ImageRequest
import coil.request.ImageResult

class ImageLoaderIdlingResource : EventListener, IdlingResource {

    private val ongoingRequests = mutableSetOf<ImageRequest>()

    var startedRequests = 0
        private set

    var finishedRequests = 0
        private set

    override val isIdleNow: Boolean
        get() = ongoingRequests.isEmpty()

    override fun onStart(request: ImageRequest) {
        ongoingRequests += request
        startedRequests++
    }

    override fun onCancel(request: ImageRequest) {
        ongoingRequests -= request
    }

    override fun onError(request: ImageRequest, throwable: Throwable) {
        ongoingRequests -= request
        finishedRequests++
    }

    override fun onSuccess(request: ImageRequest, metadata: ImageResult.Metadata) {
        ongoingRequests -= request
        finishedRequests++
    }
}
