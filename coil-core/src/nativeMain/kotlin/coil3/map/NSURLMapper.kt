package coil3.map

import coil3.Uri
import coil3.request.Options
import coil3.toUri
import platform.Foundation.NSURL

internal class NSURLMapper : Mapper<Any, Uri> {
    override fun map(data: Any, options: Options): Uri? {
        // https://youtrack.jetbrains.com/issue/KT-62997
        if (data !is NSURL) return null
        return checkNotNull(data.absoluteString()).toUri()
    }
}
