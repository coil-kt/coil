# SVGs

To add SVG support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-svg:0.9.1")
```

And add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader(context) {
    componentRegistry {
        add(SvgDecoder())
    }
}
```

And that's it! The `ImageLoader` will automatically detect any SVGs using their file headers and decode them correctly.
