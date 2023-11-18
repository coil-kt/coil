package coil.request

import coil.size.Scale
import coil.size.Size
import coil.size.SizeResolver

internal actual fun ImageRequest.Builder.resolveSizeResolver(): SizeResolver {
    return SizeResolver(Size.ORIGINAL)
}

internal actual fun ImageRequest.Builder.resolveScale(): Scale {
    return Scale.FIT
}
