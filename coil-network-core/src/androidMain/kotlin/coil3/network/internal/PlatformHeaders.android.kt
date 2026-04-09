package coil3.network.internal

import android.graphics.ImageDecoder
import android.os.Build
import coil3.network.NetworkHeaders

internal actual fun getPlatformHeaders(): NetworkHeaders = NetworkHeaders.Builder().set("accept", supportedMimeTypes).build()

// Won't change during runtime
private val supportedMimeTypes by lazy {
    // https://developer.android.com/media/platform/supported-formats#image-formats
    val alwaysSupportedTypes =
        listOf("bmp;q=0.1", "gif;q=0.1", "jpeg;q=0.2", "pjpg;q=0.2", "png;q=0.3", "webp;q=0.4")
    val typesSupportedFromSpecificApi = buildList {
        // Technically previous release could also work, but we can't check for 4.2.1, only for 4.2
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add("heic;q=0.5")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ImageDecoder.isMimeTypeSupported("image/avif")) {
            add("avif;q=0.6")
        }
    }
    (alwaysSupportedTypes + typesSupportedFromSpecificApi).joinToString(",") { "image/$it" }
}
