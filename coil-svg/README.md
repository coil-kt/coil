# SVGs

To add SVG support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-svg:2.2.2")
```

And add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(SvgDecoder.Factory())
    }
    .build()
```

The `ImageLoader` will automatically detect and decode any SVGs. Coil detects SVGs by looking for the `<svg ` marker in the first 1 KB of the file, which should cover most cases. If the SVG is not automatically detected, you can [set the `Decoder` explicitly](../api/coil-base/coil.request/-image-request/-builder/decoder-factory) to `SvgDecoder` for the request.
