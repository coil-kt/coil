package coil.size

internal class RealSizeResolver(private val size: Size) : SizeResolver {

    override suspend fun size() = size

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is RealSizeResolver && size == other.size)
    }

    override fun hashCode() = size.hashCode()

    override fun toString() = "RealSizeResolver(size=$size)"
}
