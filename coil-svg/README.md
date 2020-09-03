# SVGs

To add SVG support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-svg:0.13.0")
```

And add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .componentRegistry {
        add(SvgDecoder())
    }
    .build()
```

The `ImageLoader` will automatically detect and decode any SVGs if the request's MIME type is `image/svg+xml`. The MIME type is inferred using the HTTP `content-type` header, a URI's suffix, or a file's extension. If you need to force a specific request to use the `SvgDecoder`, you can [set the `Fetcher` explicitly](../api/coil-base/coil.request/-request-builder/fetcher/) for the request.
