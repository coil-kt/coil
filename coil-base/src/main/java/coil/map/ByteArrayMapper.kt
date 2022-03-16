package coil.map

import coil.request.Options
import java.nio.ByteBuffer

internal class ByteArrayMapper : Mapper<ByteArray, ByteBuffer> {

    override fun map(data: ByteArray, options: Options): ByteBuffer = ByteBuffer.wrap(data)
}
