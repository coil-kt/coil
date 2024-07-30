package coil3

import android.net.Uri as AndroidUri
import androidx.core.net.toUri as toAndroidUri

fun AndroidUri.toCoilUri(): Uri = toString().toUri()

fun Uri.toAndroidUri(): AndroidUri = toString().toAndroidUri()
