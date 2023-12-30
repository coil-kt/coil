package coil3.map

import coil3.Uri
import coil3.request.Options
import coil3.toUri
import coil3.util.SCHEME_FILE
import okio.Path

internal class PathMapper : Mapper<Path, Uri> {
    override fun map(data: Path, options: Options): Uri {
        return "$SCHEME_FILE://$data".toUri()
    }
}
