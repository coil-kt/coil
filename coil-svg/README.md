# SVGs

To add SVG support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-svg:0.9.5")
```

And add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .componentRegistry {
        add(SvgDecoder())
    }
    .build()
```

And that's it! The `ImageLoader` will automatically detect any SVGs and decode them correctly.
