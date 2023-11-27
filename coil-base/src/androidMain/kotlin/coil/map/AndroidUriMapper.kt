package coil.map

import android.net.Uri as AndroidUri
import coil.Uri as CoilUri
import coil.request.Options
import coil.toUri

class AndroidUriMapper : Mapper<AndroidUri, CoilUri> {
    override fun map(data: AndroidUri, options: Options): CoilUri {
        return data.toString().toUri()
    }
}
