package coil.map

import coil.Uri
import coil.request.Options
import coil.toUri
import coil.util.SCHEME_FILE
import okio.Path

internal class PathMapper : Mapper<Path, Uri> {
    override fun map(data: Path, options: Options): Uri {
        return "$SCHEME_FILE://$data".toUri()
    }
}
