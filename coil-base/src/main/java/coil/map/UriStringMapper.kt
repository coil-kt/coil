package coil.map

import android.content.ContentResolver
import android.net.Uri

internal class UriStringMapper : Mapper<String, Uri> {

    override fun map(data: String): Uri = Uri.parse(data).let {
        return if (it.scheme != null) {
            it
        } else {
            it.buildUpon().scheme(ContentResolver.SCHEME_FILE).build()
        }
    }
}
