package coil3.map

import coil3.Uri
import coil3.request.Options
import coil3.toUri
import platform.Foundation.NSURL

internal class NSURLMapper : Mapper<NSURL, Uri> {
    override fun map(data: NSURL, options: Options): Uri {
        return checkNotNull(data.absoluteString()).toUri()
    }
}
