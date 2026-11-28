package sample.common

import android.os.Build.VERSION.SDK_INT
import coil3.ComponentRegistry
import coil3.network.httpengine.HttpEngineNetworkFetcherFactory

internal actual fun ComponentRegistry.Builder.addPlatformComponents() {
    if (SDK_INT >= 34) {
        add(HttpEngineNetworkFetcherFactory())
    }
}
