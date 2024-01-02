package coil3.decode

import coil3.annotation.ExperimentalCoilApi
import java.nio.ByteBuffer

/**
 * Metadata containing the underlying [ByteBuffer] of the [ImageSource].
 */
@ExperimentalCoilApi
class ByteBufferMetadata(
    val byteBuffer: ByteBuffer,
) : ImageSource.Metadata()
