package coil.map

import android.net.Uri
import androidx.core.net.toUri
import java.io.File

internal class FileMapper : Mapper<File, Uri> {

    override fun map(data: File): Uri = data.toUri()
}
