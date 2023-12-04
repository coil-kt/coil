package coil3.compose

import androidx.compose.ui.test.IdlingResource
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import java.util.Collections

class ImageLoaderIdlingResource : EventListener(), IdlingResource {

    private val ongoingRequests = Collections.synchronizedSet(mutableSetOf<ImageRequest>())
    private val _results = Collections.synchronizedList(mutableListOf<ImageResult>())

    val results: List<ImageResult>
        get() = _results.toList()

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
        _results += result
        finishedRequests++
    }

    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
        ongoingRequests -= request
        _results += result
        finishedRequests++
    }
}
