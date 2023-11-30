package sample.common

import okio.Source
import okio.source

class JvmResources : Resources {
    override fun open(path: String): Source {
        return javaClass.classLoader!!.getResourceAsStream(path)!!.source()
    }
}
