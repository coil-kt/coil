package coil.network

import android.os.Looper
import android.os.NetworkOnMainThreadException

internal actual fun assertNotOnMainThread() {
    if (isMainThread()) {
        throw NetworkOnMainThreadException()
    }
}

internal fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()
