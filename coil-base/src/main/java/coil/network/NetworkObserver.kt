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
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import coil.network.NetworkObserver.Listener
import coil.util.Logger
import coil.util.isPermissionGranted
import coil.util.log

/**
 * Observes the device's network state and calls [Listener] if any state changes occur.
 *
 * This class provides a raw stream of updates from the network APIs. The [Listener] can be
 * called multiple times for the same network state.
 */
internal interface NetworkObserver {

    companion object {
        private const val TAG = "NetworkObserver"

        /** Create a new [NetworkObserver] instance. */
        operator fun invoke(context: Context, listener: Listener, logger: Logger?): NetworkObserver {
            val connectivityManager: ConnectivityManager? = context.getSystemService()
            if (connectivityManager == null || !context.isPermissionGranted(ACCESS_NETWORK_STATE)) {
                logger?.log(TAG, Log.WARN) { "Unable to register network observer." }
                return EmptyNetworkObserver
            }

            return try {
                if (SDK_INT >= 21) {
                    NetworkObserverApi21(connectivityManager, listener)
                } else {
                    NetworkObserverApi14(context, connectivityManager, listener)
                }
            } catch (e: Exception) {
                logger?.log(TAG, RuntimeException("Failed to register network observer.", e))
                EmptyNetworkObserver
            }
        }
    }

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

private object EmptyNetworkObserver : NetworkObserver {

    override val isOnline get() = true

    override fun shutdown() {}
}

@RequiresApi(21)
@SuppressLint("MissingPermission")
private class NetworkObserverApi21(
    private val connectivityManager: ConnectivityManager,
    private val listener: Listener
) : NetworkObserver {

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onConnectivityChange(network, true)
        override fun onLost(network: Network) = onConnectivityChange(network, false)
    }

    override val isOnline: Boolean
        get() = connectivityManager.allNetworks.any { it.isOnline() }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
        val capabilities: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(this)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
private class NetworkObserverApi14(
    private val context: Context,
    private val connectivityManager: ConnectivityManager,
    listener: Listener
) : NetworkObserver {

    private val connectionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                listener.onConnectivityChange(isOnline)
            }
        }
    }

    override val isOnline: Boolean
        get() = connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true

    init {
        context.registerReceiver(connectionReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun shutdown() {
        context.unregisterReceiver(connectionReceiver)
    }
}
