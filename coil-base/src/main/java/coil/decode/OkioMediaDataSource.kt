package coil.decode

import android.media.MediaDataSource
import android.os.Build
import androidx.annotation.RequiresApi
import okio.FileSystem
import okio.Path

@RequiresApi(Build.VERSION_CODES.M)
internal class OkioMediaDataSource(private val file: Path, private val fileSystem: FileSystem) :
    MediaDataSource() {
    val fileHandle = fileSystem.openReadOnly(file)

    override fun close() {
        fileHandle.close()
    }

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        return fileHandle.read(position, buffer, offset, size)
    }

    override fun getSize(): Long {
        return fileSystem.metadata(file).size ?: -1
    }
}
