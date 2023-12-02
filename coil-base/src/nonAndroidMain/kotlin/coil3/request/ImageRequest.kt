package coil3.request

import coil3.size.Scale
import coil3.size.Size
import coil3.size.SizeResolver

internal actual fun ImageRequest.Builder.resolveSizeResolver(): SizeResolver {
    return SizeResolver(Size.ORIGINAL)
}

internal actual fun ImageRequest.Builder.resolveScale(): Scale {
    return Scale.FIT
}
