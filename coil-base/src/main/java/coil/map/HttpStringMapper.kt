package coil.map

import okhttp3.HttpUrl

internal class HttpStringMapper : Mapper<String, HttpUrl> {

    override fun handles(data: String): Boolean = HttpUrl.parse(data) != null

    override fun map(data: String): HttpUrl = HttpUrl.get(data)
}
