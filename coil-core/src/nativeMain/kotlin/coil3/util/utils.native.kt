package coil3.util

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

@OptIn(ExperimentalNativeApi::class)
internal actual typealias WeakReference<T> = kotlin.native.ref.WeakReference<T>

@OptIn(ExperimentalNativeApi::class)
internal actual fun Any.identityHashCode() = identityHashCode()
