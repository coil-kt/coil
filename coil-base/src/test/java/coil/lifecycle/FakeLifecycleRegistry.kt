package coil.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry

internal class FakeLifecycleRegistry(
    private val lifecycle: Lifecycle = FakeLifecycle()
) : LifecycleRegistry({ lifecycle })
