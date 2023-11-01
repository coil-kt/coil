package coil.util

import coil.annotation.MainThread
import coil.util.NetworkObserver.Listener

/**
 * Observes the device's network state and calls [Listener] if any state changes occur.
 *
 * This class provides a raw stream of updates from the network APIs. The [Listener] can be
 * called multiple times for the same network state.
 */
internal interface NetworkObserver {

    /** Synchronously checks if the device is online. */
    val isOnline: Boolean

    /** Stop observing network changes. */
    fun shutdown()

    /** Calls [onConnectivityChange] when a connectivity change event occurs. */
    fun interface Listener {

        @MainThread
        fun onConnectivityChange(isOnline: Boolean)
    }
}

internal class EmptyNetworkObserver : NetworkObserver {

    override val isOnline get() = true

    override fun shutdown() {}
}
