package coil3.key

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import coil3.Uri
import coil3.request.Options
import coil3.util.nightMode

internal class AndroidResourceUriKeyer : Keyer<Uri> {

    override fun key(data: Uri, options: Options): String? {
        if (data.scheme == SCHEME_ANDROID_RESOURCE) {
            // 'android.resource' uris can change if night mode is enabled/disabled.
            return "$data:${options.context.resources.configuration.nightMode}"
        }
        return null
    }
}
