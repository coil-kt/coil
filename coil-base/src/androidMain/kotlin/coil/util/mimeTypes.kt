package coil.util

import android.webkit.MimeTypeMap

internal actual fun extensionFromMimeTypeMap(extension: String): String? {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}
