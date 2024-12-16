package coil3.intercept

import coil3.Bitmap
import coil3.BitmapImage
import coil3.Image
import coil3.request.Options
import coil3.transform.Transformation
import coil3.util.Logger

internal actual fun convertImageToBitmap(
    image: Image,
    options: Options,
    transformations: List<Transformation>,
    logger: Logger?,
): Bitmap {
    if (image is BitmapImage) {
        return image.bitmap
    }

    error(
        "Converting image of type ${image::class.simpleName} " +
            "to apply transformations: $transformations is not currently supported on" +
            "non-Android platforms. Set ImageRequest.Builder.allowConversionToBitmap(false) to " +
            "skip applying these transformations."
    )
}
