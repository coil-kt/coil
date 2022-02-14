package coil.size

internal class RealScaleResolver(private val scale: Scale) : ScaleResolver {

    override fun scale() = scale

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RealScaleResolver && scale == other.scale
    }

    override fun hashCode() = scale.hashCode()
}
