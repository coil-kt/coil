package coil3.util

import coil3.annotation.InternalCoilApi
import coil3.decode.Decoder
import coil3.fetch.Fetcher
import kotlin.reflect.KClass

@InternalCoilApi
expect object ServiceLoaderComponentRegistry {
    val fetchers: List<FetcherServiceLoaderTarget<*>>
    val decoders: List<DecoderServiceLoaderTarget>
}

@InternalCoilApi
interface FetcherServiceLoaderTarget<T : Any> {
    fun factory(): Fetcher.Factory<T>?
    fun type(): KClass<T>?
    fun priority(): Int = 0
}

@InternalCoilApi
interface DecoderServiceLoaderTarget {
    fun factory(): Decoder.Factory?
    fun priority(): Int = 0
}
