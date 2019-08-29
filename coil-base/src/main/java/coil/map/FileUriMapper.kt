package coil.map

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toFile
import coil.fetch.AssetUriFetcher
import java.io.File

internal class FileUriMapper : Mapper<Uri, File> {

    override fun handles(data: Uri): Boolean {
        return data.scheme == ContentResolver.SCHEME_FILE &&
            data.path.let { it != null && !it.startsWith(AssetUriFetcher.ASSET_FILE_PATH_ROOT) }
    }

    override fun map(data: Uri) = data.toFile()
}
