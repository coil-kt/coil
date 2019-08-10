# Gifs

Unlike Glide, GIFs are not supported by default. However, Coil has an extension library to support them.

To add GIF support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-gif:0.6.0")
```

And add the decoders to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader(context) {
    componentRegistry {
        if (SDK_INT >= P) {
            add(ImageDecoderDecoder())
        } else {
            add(GifDecoder())
        }
    }
}
```

!!! Note
    Prefer using `ImageDecoderDecoder` on Android P and above, as it's backed by Android P's new [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder) API. This provides native support for GIFs **and Animated WebPs**.

And that's it! The `ImageLoader` will automatically detect any GIFs using their file headers and decode them correctly.
