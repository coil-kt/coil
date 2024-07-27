package coil3.util

import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual typealias WeakReference<T> = kotlin.native.ref.WeakReference<T>
