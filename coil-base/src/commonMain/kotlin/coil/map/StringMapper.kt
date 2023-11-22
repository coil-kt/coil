package coil.map

import coil.Uri
import coil.request.Options
import coil.toUri

internal class StringMapper : Mapper<String, Uri> {
    override fun map(data: String, options: Options): Uri {
        return data.toUri()
    }
}
