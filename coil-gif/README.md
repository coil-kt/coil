# Gifs

Unlike Glide, GIFs are not supported by default. However, Coil has an extension library to support them.

To add GIF support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-gif:1.1.1")
```

And add the decoders to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .componentRegistry {
        if (SDK_INT >= Build.VERSION_CODES.P) {
            add(ImageDecoderDecoder(context))
        } else {
            add(GifDecoder())
        }
    }
    .build()
```

And that's it! The `ImageLoader` will automatically detect any GIFs using their file headers and decode them correctly.

To transform the pixel data of each frame of a GIF, see [AnimatedTransformation](../api/coil-gif/coil.transform/-animated-transformation/).

!!! Note
    Coil includes two separate decoders to support decoding GIFs. `GifDecoder` supports all API levels, but is slower. `ImageDecoderDecoder` is powered by Android's new [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder) API which is only available on API 28 and above. `ImageDecoderDecoder` is faster than `GifDecoder` and supports decoding animated WebP images and animated HEIF image sequences.
