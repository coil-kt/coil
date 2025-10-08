package coil3.video.internal

import android.media.MediaDataSource
import androidx.annotation.RequiresApi
import okio.ByteString

@RequiresApi(23)
internal class ByteStringMediaDataSource(
    private val data: ByteString,
) : MediaDataSource() {

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position < 0L || position > Int.MAX_VALUE.toLong()) return -1

        val startIndex = position.toInt()
        if (startIndex >= data.size) return -1

        val available = data.size - startIndex
        val length = minOf(size, available)
        if (length <= 0) return 0

        var targetIndex = offset
        for (i in 0 until length) {
            buffer[targetIndex++] = data[startIndex + i]
        }
        return length
    }

    override fun getSize(): Long {
        return data.size.toLong()
    }

    override fun close() {}
}
