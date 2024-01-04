package coil3.util

import coil3.annotation.InternalCoilApi
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

@InternalCoilApi
actual object ServiceLoaderComponentRegistry {

    private val lock = SynchronizedObject()
    private val _fetchers = mutableListOf<FetcherServiceLoaderTarget<*>>()
    private val _decoders = mutableListOf<DecoderServiceLoaderTarget>()

    actual val fetchers: List<FetcherServiceLoaderTarget<*>>
        get() = synchronized(lock) { _fetchers.toImmutableList() }

    actual val decoders: List<DecoderServiceLoaderTarget>
        get() = synchronized(lock) { _decoders.toImmutableList() }

    fun register(fetcher: FetcherServiceLoaderTarget<*>) = synchronized(lock) {
        _fetchers += fetcher
    }

    fun register(decoder: DecoderServiceLoaderTarget) = synchronized(lock) {
        _decoders += decoder
    }
}
