package coil3.util

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.identityHashCode

@OptIn(ExperimentalNativeApi::class)
@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual typealias WeakReference<T> = kotlin.native.ref.WeakReference<T>

@OptIn(ExperimentalNativeApi::class)
internal actual fun Any.identityHashCode() = identityHashCode()
