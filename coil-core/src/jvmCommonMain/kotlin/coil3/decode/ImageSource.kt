package coil3.decode

import coil3.annotation.ExperimentalCoilApi
import java.nio.ByteBuffer

/**
 * Metadata containing the [bytebuffer] of a ByteBuffer, maybe direct
 */
@ExperimentalCoilApi
class ByteBufferMetadata(
    val byteBuffer: ByteBuffer
) : ImageSource.Metadata()
