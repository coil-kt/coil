package coil3.network.internal

import coil3.annotation.InternalCoilApi
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

@InternalCoilApi
fun <P, T> singleParameterLazy(initializer: (P) -> T) = SingleParameterLazy(initializer)

@InternalCoilApi
class SingleParameterLazy<P, T>(initializer: (P) -> T) : SynchronizedObject() {
    private var initializer: ((P) -> T)? = initializer
    private var _value: Any? = UNINITIALIZED

    @Suppress("UNCHECKED_CAST")
    fun get(parameter: P): T {
        val value1 = _value
        if (value1 !== UNINITIALIZED) {
            return value1 as T
        }

        return synchronized(this) {
            val value2 = _value
            if (value2 !== UNINITIALIZED) {
                value2 as T
            } else {
                val newValue = initializer!!(parameter)
                _value = newValue
                initializer = null
                newValue
            }
        }
    }
}

private object UNINITIALIZED
