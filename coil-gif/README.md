# Gifs

Unlike Glide, GIFs are not supported by default. However, Coil has an extension library to support them.

To add GIF support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-gif:0.13.0")
```

And add the decoders to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .componentRegistry {
        if (SDK_INT >= 28) {
            add(ImageDecoderDecoder())
        } else {
            add(GifDecoder())
        }
    }
    .build()
```

And that's it! The `ImageLoader` will automatically detect any GIFs using their file headers and decode them correctly.

!!! Note
    Coil includes two separate decoders to support decoding GIFs. `GifDecoder` supports all API levels, but is slower. `ImageDecoderDecoder` is powered by Android's new [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder) API which is only available on API 28 and above. `ImageDecoderDecoder` is faster than `GifDecoder` and supports decoding animated WebP images and animated HEIF image sequences.
