# Gifs

Unlike Glide, GIFs are not supported by default. However, Coil has an extension library to support them.

To add GIF support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-gif:0.9.0")
```

And add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader(context) {
    componentRegistry {
        add(GifDecoder())
    }
}
```

And that's it! The `ImageLoader` will automatically detect any GIFs using their file headers and decode them correctly.
