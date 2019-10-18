package coil.decode

import android.media.MediaDataSource
import android.os.Build.VERSION_CODES.M
import androidx.annotation.RequiresApi
import okio.BufferedSource

@RequiresApi(M)
internal class BufferedMediaDataSource(private val source: BufferedSource) : MediaDataSource() {

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        return source.peek()
            .apply { skip(position) }
            .run { read(buffer, offset, size) }
    }

    override fun getSize() = -1L

    override fun close() = source.close()
}
