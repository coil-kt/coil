package coil.interceptor

import coil.EventListener
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageRequest
import coil.request.NullRequestData
import coil.request.RequestResult
import coil.size.Size

@OptIn(ExperimentalCoilApi::class)
internal class RealInterceptorChain(
    val initialRequest: ImageRequest,
    val requestType: Int,
    val interceptors: List<Interceptor>,
    val index: Int,
    override val request: ImageRequest,
    override val size: Size,
    val eventListener: EventListener
) : Interceptor.Chain {

    override fun withSize(size: Size) = copy(size = size)

    override suspend fun proceed(request: ImageRequest): RequestResult {
        if (index > 0) checkRequest(request, interceptors[index - 1])
        val interceptor = interceptors[index]
        val next = copy(index = index + 1, request = request)
        val result = interceptor.intercept(next)
        checkRequest(result.request, interceptor)
        return result
    }

    private fun checkRequest(request: ImageRequest, interceptor: Interceptor) {
        check(request.data !== NullRequestData) {
            "Interceptor '$interceptor' cannot set the request's data to null."
        }
        check(request.target === initialRequest.target) {
            "Interceptor '$interceptor' cannot modify the request's target."
        }
        check(request.lifecycle === initialRequest.lifecycle) {
            "Interceptor '$interceptor' cannot modify the request's lifecycle."
        }
        check(request.sizeResolver === initialRequest.sizeResolver) {
            "Interceptor '$interceptor' cannot modify the request's size resolver."
        }
    }

    private fun copy(
        index: Int = this.index,
        request: ImageRequest = this.request,
        size: Size = this.size
    ) = RealInterceptorChain(initialRequest, requestType, interceptors, index, request, size, eventListener)
}
