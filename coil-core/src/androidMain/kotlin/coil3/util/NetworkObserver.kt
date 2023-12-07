package coil3.util

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest
import androidx.core.content.getSystemService

private const val TAG = "NetworkObserver"

/** Create a new [NetworkObserver]. */
internal fun NetworkObserver(
    context: Context,
    listener: NetworkObserver.Listener,
    logger: Logger?,
): NetworkObserver {
    val connectivityManager: ConnectivityManager? = context.getSystemService()
    if (connectivityManager == null || !context.isPermissionGranted(ACCESS_NETWORK_STATE)) {
        logger?.log(TAG, Logger.Level.Warn) { "Unable to register network observer." }
        return EmptyNetworkObserver()
    }

    return try {
        RealNetworkObserver(connectivityManager, listener)
    } catch (e: Exception) {
        logger?.log(TAG, RuntimeException("Failed to register network observer.", e))
        EmptyNetworkObserver()
    }
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION") // TODO: Remove uses of 'allNetworks'.
private class RealNetworkObserver(
    private val connectivityManager: ConnectivityManager,
    private val listener: NetworkObserver.Listener
) : NetworkObserver {

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onConnectivityChange(network, true)
        override fun onLost(network: Network) = onConnectivityChange(network, false)
    }

    override val isOnline: Boolean
        get() = connectivityManager.allNetworks.any { it.isOnline() }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun shutdown() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun onConnectivityChange(network: Network, isOnline: Boolean) {
        val isAnyOnline = connectivityManager.allNetworks.any {
            if (it == network) {
                // Don't trust the network capabilities for the network that just changed.
                isOnline
            } else {
                it.isOnline()
            }
        }
        listener.onConnectivityChange(isAnyOnline)
    }

    private fun Network.isOnline(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(this)
        return capabilities != null && capabilities.hasCapability(NET_CAPABILITY_INTERNET)
    }
}
