package coil.map

import android.net.Uri
import androidx.core.net.toUri
import coil.request.Options

internal class StringMapper : Mapper<String, Uri> {

    override fun map(data: String, options: Options) = data.toUri()
}
