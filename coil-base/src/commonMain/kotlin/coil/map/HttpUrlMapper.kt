package coil.map

import coil.request.Options
import okhttp3.HttpUrl

internal class HttpUrlMapper : Mapper<HttpUrl, String> {

    override fun map(data: HttpUrl, options: Options) = data.toString()
}
