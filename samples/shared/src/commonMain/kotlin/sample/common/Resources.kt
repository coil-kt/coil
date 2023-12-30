package sample.common

import okio.Source

interface Resources {
    val root: String
    suspend fun open(path: String): Source
}
