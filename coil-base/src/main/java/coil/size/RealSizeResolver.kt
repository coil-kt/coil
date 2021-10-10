package coil.size

internal class RealSizeResolver(private val size: Size) : SizeResolver {

    override suspend fun size(): Size = size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RealSizeResolver && size == other.size
    }

    override fun hashCode(): Int = size.hashCode()
}
