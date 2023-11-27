package coil.util

import kotlin.experimental.ExperimentalNativeApi

@ExperimentalNativeApi // This must be propagated from the underlying implementation.
internal expect class WeakReference<T : Any>(referred: T) {
    fun get(): T?
}

internal expect fun Any.identityHashCode(): Int
