package coil.map

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import coil.request.Options

/**
 * Maps android.resource uris with resource names to uris containing their resources ID. i.e.:
 *
 * android.resource://example.package.name/drawable/image -> android.resource://example.package.name/12345678
 */
internal class ResourceUriMapper : Mapper<Uri, Uri> {

    override fun map(data: Uri, options: Options): Uri? {
        if (!isApplicable(data)) return null

        val packageName = data.authority.orEmpty()
        val resources = options.context.packageManager.getResourcesForApplication(packageName)
        val (type, name) = data.pathSegments
        val id = resources.getIdentifier(name, type, packageName)
        check(id != 0) { "Invalid ${ContentResolver.SCHEME_ANDROID_RESOURCE} URI: $data" }

        return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://$packageName/$id".toUri()
    }

    private fun isApplicable(data: Uri): Boolean {
        return data.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE &&
            !data.authority.isNullOrBlank() &&
            data.pathSegments.count() == 2
    }
}
