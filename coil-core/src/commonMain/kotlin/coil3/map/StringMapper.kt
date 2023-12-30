package coil3.map

import coil3.Uri
import coil3.request.Options
import coil3.toUri

internal class StringMapper : Mapper<String, Uri> {
    override fun map(data: String, options: Options): Uri {
        return data.toUri()
    }
}
