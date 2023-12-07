package coil3.map

import coil3.Uri
import coil3.request.Options
import coil3.toUri
import coil3.util.SCHEME_FILE
import java.io.File

class FileMapper : Mapper<File, Uri> {
    override fun map(data: File, options: Options): Uri {
        return "$SCHEME_FILE://$data".toUri()
    }
}
