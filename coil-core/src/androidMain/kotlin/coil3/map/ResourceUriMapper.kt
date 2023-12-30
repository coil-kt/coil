package coil3.map

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import coil3.Uri
import coil3.pathSegments
import coil3.request.Options
import coil3.toUri

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
        //noinspection DiscouragedApi: Necessary to support resource URIs.
        val id = resources.getIdentifier(name, type, packageName)
        check(id != 0) { "Invalid $SCHEME_ANDROID_RESOURCE URI: $data" }

        return "$SCHEME_ANDROID_RESOURCE://$packageName/$id".toUri()
    }

    private fun isApplicable(data: Uri): Boolean {
        return data.scheme == SCHEME_ANDROID_RESOURCE &&
            !data.authority.isNullOrBlank() &&
            data.pathSegments.count() == 2
    }
}
