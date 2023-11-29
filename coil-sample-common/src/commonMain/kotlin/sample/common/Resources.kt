package sample.common

import okio.Source

fun interface Resources {
    fun open(path: String): Source
}
