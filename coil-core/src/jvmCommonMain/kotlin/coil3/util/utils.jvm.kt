package coil3.util

internal actual typealias WeakReference<T> = java.lang.ref.WeakReference<T>

internal actual fun Any.identityHashCode() = System.identityHashCode(this)
