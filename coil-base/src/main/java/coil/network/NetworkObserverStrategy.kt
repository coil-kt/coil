package coil.network

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import coil.network.NetworkObserverStrategy.Listener
import coil.util.Logger
import coil.util.isPermissionGranted
import coil.util.log

/**
 * Checks if the device is online and calls a [Listener] for any network state change events.
 *
 * This class provides a raw stream of updates from the network APIs. The [Listener] can be called multiple times
 * for the same network state.
 *
 * @see NetworkObserver
 */
internal interface NetworkObserverStrategy {

    companion object {
        private const val TAG = "NetworkObserverStrategy"

        /** Create a new [NetworkObserverStrategy] instance. */
        operator fun invoke(context: Context, listener: Listener, logger: Logger?): NetworkObserverStrategy {
            val connectivityManager: ConnectivityManager? = context.getSystemService()
            if (connectivityManager == null || !context.isPermissionGranted(ACCESS_NETWORK_STATE)) {
                logger?.log(TAG, Log.WARN) { "Unable to register network observer." }
                return EmptyNetworkObserverStrategy
            }

            return try {
                if (SDK_INT >= 21) {
                    NetworkObserverStrategyApi21(connectivityManager, listener)
                } else {
                    NetworkObserverStrategyApi14(context, connectivityManager, listener)
                }
            } catch (e: Exception) {
                logger?.log(TAG, RuntimeException("Failed to register network observer.", e))
                EmptyNetworkObserverStrategy
            }
        }
    }

    /** Calls [onConnectivityChange] when a connectivity change event occurs. */
    interface Listener {
        fun onConnectivityChange(isOnline: Boolean)
    }

    /** Start observing network changes. */
    fun start()

    /** Stop observing network changes. */
    fun stop()

    /** Synchronously checks if the device is online. */
    fun isOnline(): Boolean
}

private object EmptyNetworkObserverStrategy : NetworkObserverStrategy {

    override fun start() {}

    override fun stop() {}

    override fun isOnline() = true
}

@RequiresApi(21)
@SuppressLint("MissingPermission")
private class NetworkObserverStrategyApi21(
    private val connectivityManager: ConnectivityManager,
    private val listener: Listener
) : NetworkObserverStrategy {

    companion object {
        private val NETWORK_REQUEST = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onConnectivityChange(network, true)
        override fun onLost(network: Network) = onConnectivityChange(network, false)
    }

    override fun start() {
        connectivityManager.registerNetworkCallback(NETWORK_REQUEST, networkCallback)
    }

    override fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun isOnline(): Boolean {
        return connectivityManager.allNetworks.any { it.isOnline() }
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
        val capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(this)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
private class NetworkObserverStrategyApi14(
    private val context: Context,
    private val connectivityManager: ConnectivityManager,
    listener: Listener
) : NetworkObserverStrategy {

    companion object {
        private val INTENT_FILTER = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
    }

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                listener.onConnectivityChange(isOnline())
            }
        }
    }

    override fun start() {
        context.registerReceiver(connectionReceiver, INTENT_FILTER)
    }

    override fun stop() {
        context.unregisterReceiver(connectionReceiver)
    }

    override fun isOnline(): Boolean {
        return connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
    }
}
