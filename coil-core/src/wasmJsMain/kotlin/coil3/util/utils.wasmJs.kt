package coil3.util

import kotlin.js.JsAny
import kotlin.js.JsReference
import kotlin.js.toJsReference

internal actual class WeakReference<T : Any> actual constructor(referred: T) {
    private var reference: WeakRef? = WeakRef(referred.toJsReference())

    actual fun get(): T? {
        return reference?.deref()
            ?.unsafeCast<JsReference<T>>()
            ?.get()
    }

    actual fun clear() {
        reference = null
    }
}

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakRef
private external class WeakRef(target: JsAny) {
    fun deref(): JsAny?
}

// TODO: This can be incorrect if the object implements a custom hashCode.
internal actual fun Any.identityHashCode(): Int = hashCode()
