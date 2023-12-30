package coil3.map

import android.net.Uri as AndroidUri
import coil3.Uri as CoilUri
import coil3.request.Options
import coil3.toCoilUri

class AndroidUriMapper : Mapper<AndroidUri, CoilUri> {
    override fun map(data: AndroidUri, options: Options): CoilUri {
        return data.toCoilUri()
    }
}
