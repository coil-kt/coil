package coil.intercept

import coil.EventListener
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.NullRequestData
import coil.size.Scale
import coil.size.Size

internal class RealInterceptorChain(
    val initialRequest: ImageRequest,
    val interceptors: List<Interceptor>,
    val index: Int,
    override val request: ImageRequest,
    override val size: Size,
    override val scale: Scale,
    val eventListener: EventListener,
    val isPlaceholderCached: Boolean,
) : Interceptor.Chain {

    override fun withSize(size: Size) = copy(size = size)

    override fun withScale(scale: Scale) = copy(scale = scale)

    override suspend fun proceed(request: ImageRequest): ImageResult {
        if (index > 0) checkRequest(request, interceptors[index - 1])
        val interceptor = interceptors[index]
        val next = copy(index = index + 1, request = request)
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
        check(request.lifecycle === initialRequest.lifecycle) {
            "Interceptor '$interceptor' cannot modify the request's lifecycle."
        }
        check(request.sizeResolver === initialRequest.sizeResolver) {
            "Interceptor '$interceptor' cannot modify the request's size resolver. " +
                "Use `Interceptor.Chain.withSize` instead."
        }
        check(request.scaleResolver === initialRequest.scaleResolver) {
            "Interceptor '$interceptor' cannot modify the request's scale resolver. " +
                "Use `Interceptor.Chain.withScale` instead."
        }
    }

    private fun copy(
        index: Int = this.index,
        request: ImageRequest = this.request,
        size: Size = this.size,
        scale: Scale = this.scale,
    ) = RealInterceptorChain(initialRequest, interceptors, index, request, size, scale, eventListener, isPlaceholderCached)
}
