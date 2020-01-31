package coil.map

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri

/**
 * Maps android.resource uris with resource names to uris containing their resources ID. i.e.:
 *
 * android.resource://example.package.name/drawable/image -> android.resource://example.package.name/12345678
 */
internal class ResourceUriMapper(private val context: Context) : Mapper<Uri, Uri> {

    override fun handles(data: Uri): Boolean {
        return data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE &&
            !data.authority.isNullOrBlank() &&
            data.pathSegments.count() == 2
    }

    override fun map(data: Uri): Uri {
        val packageName = data.authority.orEmpty()
        val resources = context.packageManager.getResourcesForApplication(packageName)
        val (type, name) = data.pathSegments
        val id = resources.getIdentifier(name, type, packageName)
        check(id != 0) { "Invalid ${ContentResolver.SCHEME_ANDROID_RESOURCE} URI: $data" }

        return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$id".toUri()
    }
}
