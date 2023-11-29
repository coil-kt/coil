package coil.map

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.Context
import android.content.res.Resources
import androidx.annotation.DrawableRes
import coil.Uri
import coil.request.Options
import coil.toUri

internal class ResourceIntMapper : Mapper<Int, Uri> {

    override fun map(@DrawableRes data: Int, options: Options): Uri? {
        if (!isApplicable(data, options.context)) return null
        return "$SCHEME_ANDROID_RESOURCE://${options.context.packageName}/$data".toUri()
    }

    private fun isApplicable(@DrawableRes data: Int, context: Context): Boolean {
        return try {
            context.resources.getResourceEntryName(data) != null
        } catch (_: Resources.NotFoundException) {
            false
        }
    }
}
