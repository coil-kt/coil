package coil.map

import android.net.Uri
import androidx.core.net.toUri

internal class StringMapper : Mapper<String, Uri> {

    override fun map(data: String) = data.toUri()
}
