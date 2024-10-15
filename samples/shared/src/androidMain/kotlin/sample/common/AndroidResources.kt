package sample.common

import android.content.Context

class AndroidResources(
    private val context: Context,
) : Resources {

    override fun uri(path: String): String {
        return "file:///android_asset/$path"
    }

    override suspend fun readBytes(path: String): ByteArray {
        return context.assets.open(path).use { it.readBytes() }
    }
}
