package coil3.size

import coil3.annotation.Data

@Data
internal class RealSizeResolver(private val size: Size) : SizeResolver {
    override suspend fun size() = size
}
