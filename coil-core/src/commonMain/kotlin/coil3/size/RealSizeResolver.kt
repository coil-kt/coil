package coil3.size

import coil3.annotation.Poko

@Poko
internal class RealSizeResolver(private val size: Size) : SizeResolver {
    override suspend fun size() = size
}
