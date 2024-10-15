package sample.common

interface Resources {
    fun uri(path: String): String
    suspend fun readBytes(path: String): ByteArray
}
