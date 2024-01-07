package coil3.video

import android.media.MediaDataSource
import androidx.annotation.RequiresApi
import java.io.File
import java.io.RandomAccessFile

@RequiresApi(23)
class FileMediaDataSource(private val file: File) : MediaDataSource() {

    private var randomAccessFile: RandomAccessFile? = null

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        synchronized(file) {
            if (randomAccessFile == null) {
                randomAccessFile = RandomAccessFile(file, "r")
            }

            if (position >= getSize()) {
                // indicates EOF
                return -1
            }

            val sizeToRead = minOf(size, (getSize() - position).toInt())
            randomAccessFile!!.seek(position)
            return randomAccessFile!!.read(buffer, offset, sizeToRead)
        }
    }

    override fun getSize(): Long {
        return file.length()
    }

    override fun close() {
        randomAccessFile?.close()
    }
}
