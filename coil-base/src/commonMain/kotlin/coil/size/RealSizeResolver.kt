package coil.size

import dev.drewhamilton.poko.Poko

@Poko
internal class RealSizeResolver(private val size: Size) : SizeResolver {
    override suspend fun size() = size
}
