package coil.intercept

import coil.EventListener
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.NullRequestData
import coil.size.Size

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
        checkRequest(request, null)
        return copy(request = request)
    }

    override fun withSize(size: Size): Interceptor.Chain {
        return copy(size = size)
    }

    override suspend fun proceed(request: ImageRequest): ImageResult {
        if (index > 0) checkRequest(request, interceptors[index - 1])
        val interceptor = interceptors[index]
        val next = copy(index = index + 1, request = request)
        val result = interceptor.intercept(next)
        checkRequest(result.request, interceptor)
        return result
    }

    private fun checkRequest(request: ImageRequest, interceptor: Interceptor?) {
        check(request.context === initialRequest.context) {
            "${errorPrefix(interceptor)} cannot modify the request's context."
        }
        check(request.data !== NullRequestData) {
            "${errorPrefix(interceptor)} cannot set the request's data to null."
        }
        check(request.target === initialRequest.target) {
            "${errorPrefix(interceptor)} cannot modify the request's target."
        }
        check(request.lifecycle === initialRequest.lifecycle) {
            "${errorPrefix(interceptor)} cannot modify the request's lifecycle."
        }
        check(request.sizeResolver === initialRequest.sizeResolver) {
            "${errorPrefix(interceptor)} cannot modify the request's size resolver. " +
                "Use `Interceptor.Chain.withSize` instead."
        }
    }

    private fun errorPrefix(interceptor: Interceptor?): String {
        if (interceptor != null) {
            return "Interceptor '$interceptor'"
        } else {
            return "'withRequest'"
        }
    }

    private fun copy(
        index: Int = this.index,
        request: ImageRequest = this.request,
        size: Size = this.size,
    ) = RealInterceptorChain(initialRequest, interceptors, index, request, size, eventListener, isPlaceholderCached)
}
