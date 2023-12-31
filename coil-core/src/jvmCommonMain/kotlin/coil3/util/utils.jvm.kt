package coil3.util

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual typealias WeakReference<T> = java.lang.ref.WeakReference<T>

internal actual fun Any.identityHashCode() = System.identityHashCode(this)
