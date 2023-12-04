package sample.common

import android.content.Context
import okio.Source
import okio.source

class AndroidResources(
    private val context: Context,
) : Resources {
    override suspend fun open(path: String): Source {
        return context.assets.open(path).source()
    }
}
