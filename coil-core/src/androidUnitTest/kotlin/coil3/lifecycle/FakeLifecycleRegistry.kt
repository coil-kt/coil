package coil3.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class FakeLifecycleRegistry(
    private val lifecycle: Lifecycle = FakeLifecycle(),
    private val owner: LifecycleOwner = object : LifecycleOwner {
        override val lifecycle get() = lifecycle
    },
) : LifecycleRegistry(owner)
