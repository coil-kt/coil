package sample.common

import okio.Source
import okio.source

class JvmResources : Resources {

    override val root: String
        get() = ""

    override suspend fun open(path: String): Source {
        return javaClass.classLoader!!.getResourceAsStream(path)!!.source()
    }
}
