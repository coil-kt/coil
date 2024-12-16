package coil3.video.internal

import android.media.MediaDataSource
import androidx.annotation.RequiresApi
import okio.FileHandle

@RequiresApi(23)
internal class FileHandleMediaDataSource(
    private val handle: FileHandle,
) : MediaDataSource() {

    override fun readAt(
        position: Long,
        buffer: ByteArray,
        offset: Int,
        size: Int,
    ): Int {
        return handle.read(position, buffer, offset, size)
    }

    override fun getSize(): Long {
        return handle.size()
    }

    override fun close() {
        handle.close()
    }
}
