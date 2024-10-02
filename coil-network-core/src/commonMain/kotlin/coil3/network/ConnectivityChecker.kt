package coil3.network

import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import kotlin.jvm.JvmField

@ExperimentalCoilApi
expect fun ConnectivityChecker(context: PlatformContext): ConnectivityChecker

/**
 * Determines if the device is able to access the internet.
 */
@ExperimentalCoilApi
fun interface ConnectivityChecker {

    /**
     * Return true if the device can access the internet. Else, return false.
     *
     * If false, reading from the network will automatically be disabled if the device is
     * offline. If a cached response is unavailable the request will fail with a
     * '504 Unsatisfiable Request' response.
     */
    fun isOnline(): Boolean

    companion object {
        /**
         * A naive [ConnectivityChecker] implementation that is always online.
         */
        @JvmField val ONLINE = ConnectivityChecker { true }
    }
}
