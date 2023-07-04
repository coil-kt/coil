package coil.request

import coil.Image
import coil.util.EMPTY_IMAGE_FACTORY
import coil.util.ioCoroutineDispatcher
import kotlin.jvm.JvmField
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

data class CachePolicies(
    val memory: CachePolicy = CachePolicy.ENABLED,
    val disk: CachePolicy = CachePolicy.ENABLED,
    val network: CachePolicy = CachePolicy.ENABLED,
) {
    companion object {
        @JvmField val DEFAULT = CachePolicies()
    }
}

data class ImageDispatchers(
    val interceptorDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    val fetcherDispatcher: CoroutineDispatcher = ioCoroutineDispatcher(),
    val decoderDispatcher: CoroutineDispatcher = ioCoroutineDispatcher(),
) {
    companion object {
        @JvmField val DEFAULT = ImageDispatchers()
    }
}

data class ImageFactories(
    val placeholder: () -> Image? = EMPTY_IMAGE_FACTORY,
    val error: () -> Image? = EMPTY_IMAGE_FACTORY,
    val fallback: () -> Image? = EMPTY_IMAGE_FACTORY,
) {
    companion object {
        @JvmField val DEFAULT = ImageFactories()
    }
}
