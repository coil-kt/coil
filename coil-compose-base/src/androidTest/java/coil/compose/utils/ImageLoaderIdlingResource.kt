package coil.compose.utils

import androidx.compose.ui.test.IdlingResource
import coil.EventListener
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.util.Collections

class ImageLoaderIdlingResource : EventListener, IdlingResource {

    private val ongoingRequests = Collections.synchronizedSet(mutableSetOf<ImageRequest>())

    @field:Volatile var startedRequests = 0
        private set

    @field:Volatile var finishedRequests = 0
        private set

    override val isIdleNow get() = ongoingRequests.isEmpty()

    override fun onStart(request: ImageRequest) {
        ongoingRequests += request
        startedRequests++
    }

    override fun onCancel(request: ImageRequest) {
        ongoingRequests -= request
    }

    override fun onError(request: ImageRequest, result: ErrorResult) {
        ongoingRequests -= request
        finishedRequests++
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        ongoingRequests -= request
        finishedRequests++
    }
}
