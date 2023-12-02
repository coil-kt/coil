package coil3.util

import java.lang.ref.WeakReference

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual typealias WeakReference<T> = WeakReference<T>

internal actual fun Any.identityHashCode() = System.identityHashCode(this)
