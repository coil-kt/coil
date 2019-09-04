package coil.map

import android.net.Uri
import okhttp3.HttpUrl

internal class HttpUriMapper : Mapper<Uri, HttpUrl> {

    override fun handles(data: Uri) = data.scheme == "http" || data.scheme == "https"

    override fun map(data: Uri) = HttpUrl.get(data.toString())
}
