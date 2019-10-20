package coil.decode

import android.media.MediaDataSource
import android.os.Build.VERSION_CODES.M
import androidx.annotation.RequiresApi
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import java.io.File

@RequiresApi(M)
internal class BufferedMediaDataSource(private val source: BufferedSource) : MediaDataSource() {

    companion object {
        private const val MAX_MEMORY_BUFFER_SIZE: Long = 10 * 1024 * 1024 // 10MB
    }

    private var fileSize = 0L
    private var file: File? = null
    private var sink: BufferedSink? = null

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        val bytesRead = source.peek()
            .apply { skip(position) }
            .run { read(buffer, offset, size) }

        // Avoid buffering large videos into memory by writing to a temp file if
        // the memory buffer grows too large.
        if (source.buffer.size > MAX_MEMORY_BUFFER_SIZE) {
            val fileBuffer = sink ?: run {
                val tempFile = createTempFile().also { file = it }
                tempFile.sink().buffer().also { sink = it }
            }
            fileSize += bytesRead.toLong()
            source.buffer.copyTo(fileBuffer.buffer, source.buffer.size - bytesRead, bytesRead.toLong())
            fileBuffer.emitCompleteSegments()
        }

        return bytesRead
    }

    override fun getSize() = -1L

    override fun close() {
        source.close()
        sink?.close()
        file?.delete()
    }
}
