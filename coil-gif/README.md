# Gifs

Unlike Glide, GIFs are not supported by default. However, Coil has an extension library to support them.

To add GIF support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-gif:0.8.0")
```

And add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader(context) {
    componentRegistry {
        add(GifDecoder())
    }
}
```

!!! Note
    `coil-gif` also includes an `ImageDecoderDecoder`, which is backed by Android P's new [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder) API. However, **it is not currently recommended to use `ImageDecoderDecoder` due to a bug in the Android framework**. You can track the status of the bug [here](https://issuetracker.google.com/issues/142335782).

And that's it! The `ImageLoader` will automatically detect any GIFs using their file headers and decode them correctly.
