package coil3.network.internal

import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Looper
import android.os.NetworkOnMainThreadException
import androidx.core.content.ContextCompat

internal actual fun assertNotOnMainThread() {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        throw NetworkOnMainThreadException()
    }
}

internal fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED
}
