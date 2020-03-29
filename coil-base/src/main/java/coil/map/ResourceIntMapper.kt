package coil.map

import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.net.toUri

internal class ResourceIntMapper(private val context: Context) : Mapper<@DrawableRes Int, Uri> {

    override fun handles(@DrawableRes data: Int) = try {
        context.resources.getResourceEntryName(data) != null
    } catch (_: Resources.NotFoundException) {
        false
    }

    override fun map(@DrawableRes data: Int): Uri {
        return "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$data".toUri()
    }
}
