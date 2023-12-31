# Transformations

Transformations allow you to modify the pixel data of an image before the `Drawable` is returned from the request.

By default, Coil comes packaged with 2 transformations: [circle crop](/coil/api/coil-core/coil3.transform/-circle-crop-transformation/) and [rounded corners](/coil/api/coil-core/coil3.transform/-rounded-corners-transformation/).

Transformations only modify the pixel data for static images. Adding a transformation to an `ImageRequest` that produces an animated image will convert it to a static image so the transformation can be applied. To transform the pixel data of each frame of an animated image, see [AnimatedTransformation](/coil/api/coil-gif/coil3.transform/-animated-transformation/).

Custom transformations should implement `equals` and `hashCode` to ensure that two `ImageRequest`s with the same properties and using the same transformation are equal.

See the [API documentation](/coil/api/coil-core/coil3.transform/-transformation/) for more information.

!!! Note
    If the `Drawable` returned by the image pipeline is not a `BitmapDrawable`, it will be converted to one. This will cause animated drawables to only draw the first frame of their animation. This behaviour can be disabled by setting `ImageRequest.Builder.allowConversionToBitmap(false)`.
