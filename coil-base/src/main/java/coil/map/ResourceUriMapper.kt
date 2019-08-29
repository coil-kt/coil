package coil.map

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.DrawableRes

class ResourceUriMapper : Mapper<Uri, @DrawableRes Int> {

    override fun handles(data: Uri) = data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE

    override fun map(data: Uri) = checkNotNull(data.lastPathSegment?.toIntOrNull()) { "Malformed resource Uri: $data" }
}
