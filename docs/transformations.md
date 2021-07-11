# Transformations

Transformations allow you to modify the pixel data of an image before the `Drawable` is returned from the request.

By default, Coil comes packaged with 4 transformations: [blur](../api/coil-base/coil.transform/-blur-transformation/), [circle crop](../api/coil-base/coil.transform/-circle-crop-transformation/), and [grayscale](../api/coil-base/coil.transform/-grayscale-transformation/), and [rounded corners](../api/coil-base/coil.transform/-rounded-corners-transformation/).

Transformations only modify the pixel data for static images. Adding a transformation to an `ImageRequest` that produces an animated image will convert it to a static image so the transformation can be applied. To transform the pixel data of each frame of an animated image, see [AnimatedTransformation](../api/coil-gif/coil-gif/coil.transform/-animated-transformation/).

See the [API doc](../api/coil-base/coil.transform/-transformation/) for more information.

!!! Note
    If the `Drawable` returned by the image pipeline is not a `BitmapDrawable`, it will be converted to one. This will cause animated drawables to only draw the first frame of their animation.
