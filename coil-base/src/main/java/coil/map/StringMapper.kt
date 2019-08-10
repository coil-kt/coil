package coil.map

import okhttp3.HttpUrl

internal class StringMapper : Mapper<String, HttpUrl> {

    override fun map(data: String): HttpUrl = HttpUrl.get(data)
}
