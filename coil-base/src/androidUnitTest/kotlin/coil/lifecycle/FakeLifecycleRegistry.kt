package coil.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class FakeLifecycleRegistry(
    private val lifecycle: Lifecycle = FakeLifecycle(),
) : LifecycleRegistry(
    object : LifecycleOwner {
        override val lifecycle get() = lifecycle
    },
)
