package coil3.network

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import coil3.PlatformContext
import coil3.network.internal.isPermissionGranted

actual fun ConnectivityChecker(context: PlatformContext): ConnectivityChecker {
    val application = context.applicationContext
    val connectivityManager: ConnectivityManager? = application.getSystemService()
    if (connectivityManager == null || !application.isPermissionGranted(ACCESS_NETWORK_STATE)) {
        return ConnectivityChecker.ONLINE
    }

    return try {
        if (SDK_INT > 23) {
            ConnectivityCheckerApi23(connectivityManager)
        } else {
            ConnectivityCheckerApi21(connectivityManager)
        }
    } catch (_: Exception) {
        ConnectivityChecker.ONLINE
    }
}

@RequiresApi(23)
@SuppressLint("MissingPermission")
private class ConnectivityCheckerApi23(
    private val connectivityManager: ConnectivityManager,
) : ConnectivityChecker {
    override fun isOnline(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities != null && capabilities.hasCapability(NET_CAPABILITY_INTERNET)
    }
}

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
private class ConnectivityCheckerApi21(
    private val connectivityManager: ConnectivityManager,
) : ConnectivityChecker {
    override fun isOnline(): Boolean {
        val info = connectivityManager.activeNetworkInfo
        return info != null && info.isConnectedOrConnecting
    }
}
