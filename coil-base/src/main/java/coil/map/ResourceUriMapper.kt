package coil.map

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes

class ResourceUriMapper(private val context: Context) : Mapper<Uri, @DrawableRes Int> {

    override fun handles(data: Uri) = data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE

    override fun map(data: Uri): Int {
        val pathSegments = data.pathSegments

        // android.resource://example.package.name/12345678
        pathSegments.lastOrNull()?.toIntOrNull()?.let { return it }

        // android.resource://example.package.name/drawable/image
        val defPackage = data.authority
        if (!defPackage.isNullOrBlank() && pathSegments.count() == 2) {
            val (defType, name) = pathSegments
            val id = context.resources.getIdentifier(name, defType, defPackage)
            if (id != 0) {
                return id
            }
        }

        throw IllegalStateException("Invalid ${ContentResolver.SCHEME_ANDROID_RESOURCE} URI: $data")
    }
}
