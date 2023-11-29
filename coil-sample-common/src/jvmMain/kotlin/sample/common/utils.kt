package sample.common

import coil.PlatformContext
import okio.Source
import okio.source

actual fun PlatformContext.openResource(name: String): Source {
    return javaClass.classLoader.getResourceAsStream(name)!!.source()
}
