package coil.interceptor

import coil.DefaultRequestOptions
import coil.EventListener
import coil.annotation.ExperimentalCoilApi
import coil.request.ImageRequest
import coil.request.RequestResult
import coil.size.Size
import coil.size.SizeResolver

@OptIn(ExperimentalCoilApi::class)
internal class RealInterceptorChain(
    val initialRequest: ImageRequest,
    val requestType: Int,
    val interceptors: List<Interceptor>,
    val index: Int,
    override val request: ImageRequest,
    override val defaults: DefaultRequestOptions,
    override val size: Size,
    val sizeResolver: SizeResolver,
    val eventListener: EventListener
) : Interceptor.Chain {

    override fun withSize(size: Size) = copy(size = size)

    override suspend fun proceed(request: ImageRequest): RequestResult {
        checkRequest(request) { interceptors[index - 1] }
        val interceptor = interceptors[index]
        val next = copy(index = index + 1, request = request)
        val result = interceptor.intercept(next)
        checkRequest(result.request) { interceptor }
        return result
    }

    private inline fun checkRequest(request: ImageRequest, interceptor: () -> Interceptor) {
        check(request.data != null) {
            "Interceptor '${interceptor()}' cannot set the request's data to null."
        }
        check(request.target === initialRequest.target) {
            "Interceptor '${interceptor()}' cannot modify the request's target."
        }
    }

    private fun copy(
        index: Int = this.index,
        request: ImageRequest = this.request,
        size: Size = this.size
    ) = RealInterceptorChain(initialRequest, requestType, interceptors, index, request, defaults, size, sizeResolver, eventListener)
}
