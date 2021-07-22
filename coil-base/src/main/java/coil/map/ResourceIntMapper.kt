package coil.map

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import coil.request.Options

internal class ResourceIntMapper : Mapper<Int, Uri> {

    override fun map(@DrawableRes data: Int, options: Options): Uri? {
        if (!isApplicable(data, options.context)) return null
        return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${options.context.packageName}/$data".toUri()
    }

    private fun isApplicable(@DrawableRes data: Int, context: Context): Boolean {
        return try {
            context.resources.getResourceEntryName(data) != null
        } catch (_: Resources.NotFoundException) {
            false
        }
    }
}
