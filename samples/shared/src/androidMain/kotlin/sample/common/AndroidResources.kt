package sample.common

import android.content.Context
import okio.Source
import okio.source

class AndroidResources(
    private val context: Context,
) : Resources {

    override val root: String
        get() = "file:///android_asset"

    override suspend fun open(path: String): Source {
        return context.assets.open(path).source()
    }
}
