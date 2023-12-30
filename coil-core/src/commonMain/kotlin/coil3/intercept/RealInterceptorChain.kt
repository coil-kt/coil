package coil3.intercept

import coil3.EventListener
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.NullRequestData
import coil3.size.Size

internal class RealInterceptorChain(
    val initialRequest: ImageRequest,
    val interceptors: List<Interceptor>,
    val index: Int,
    override val request: ImageRequest,
    override val size: Size,
    val eventListener: EventListener,
    val isPlaceholderCached: Boolean,
) : Interceptor.Chain {

    override fun withRequest(request: ImageRequest): Interceptor.Chain {
        if (index > 0) checkRequest(request, interceptors[index - 1])
        return copy(request = request)
    }

    override fun withSize(size: Size): Interceptor.Chain {
        return copy(size = size)
    }

    override suspend fun proceed(): ImageResult {
        val interceptor = interceptors[index]
        val next = copy(index = index + 1)
        val result = interceptor.intercept(next)
        checkRequest(result.request, interceptor)
        return result
    }

    private fun checkRequest(request: ImageRequest, interceptor: Interceptor) {
        check(request.context === initialRequest.context) {
            "Interceptor '$interceptor' cannot modify the request's context."
        }
        check(request.data !== NullRequestData) {
            "Interceptor '$interceptor' cannot set the request's data to null."
        }
        check(request.target === initialRequest.target) {
            "Interceptor '$interceptor' cannot modify the request's target."
        }
        check(request.sizeResolver === initialRequest.sizeResolver) {
            "Interceptor '$interceptor' cannot modify the request's size resolver. " +
                "Use `Interceptor.Chain.withSize` instead."
        }
    }

    private fun copy(
        index: Int = this.index,
        request: ImageRequest = this.request,
        size: Size = this.size,
    ) = RealInterceptorChain(
        initialRequest = initialRequest,
        interceptors = interceptors,
        index = index,
        request = request,
        size = size,
        eventListener = eventListener,
        isPlaceholderCached = isPlaceholderCached,
    )
}
