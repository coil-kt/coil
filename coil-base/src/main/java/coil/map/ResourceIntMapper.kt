package coil.map

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.net.toUri

internal class ResourceIntMapper(private val context: Context) : Mapper<@DrawableRes Int, Uri> {

    override fun map(@DrawableRes data: Int): Uri? {
        if (isApplicable(data)) return null
        return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$data".toUri()
    }

    private fun isApplicable(@DrawableRes data: Int): Boolean {
        return try {
            context.resources.getResourceEntryName(data) != null
        } catch (_: Resources.NotFoundException) {
            false
        }
    }
}
