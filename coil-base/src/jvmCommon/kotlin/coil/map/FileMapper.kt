package coil.map

import coil.Uri
import coil.request.Options
import coil.toUri
import coil.util.FILE_SCHEME
import java.io.File

class FileMapper : Mapper<File, Uri> {
    override fun map(data: File, options: Options): Uri {
        return "$FILE_SCHEME://$data".toUri()
    }
}
