package coil.key

import coil.Uri
import coil.request.Options

internal class GenericUriKeyer : Keyer<Uri> {

    override fun key(data: Uri, options: Options): String {
        return data.toString()
    }
}
