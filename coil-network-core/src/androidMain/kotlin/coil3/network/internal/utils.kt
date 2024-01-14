package coil3.network.internal

import android.os.Looper
import android.os.NetworkOnMainThreadException

internal actual fun assertNotOnMainThread() {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        throw NetworkOnMainThreadException()
    }
}
