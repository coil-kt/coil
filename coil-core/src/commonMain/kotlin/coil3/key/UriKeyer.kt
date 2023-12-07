package coil3.key

import coil3.Uri
import coil3.request.Options

internal class UriKeyer : Keyer<Uri> {

    override fun key(data: Uri, options: Options): String {
        return data.toString()
    }
}
