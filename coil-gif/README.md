# Gifs

**This feature is only available on Android.**

Unlike Glide, GIFs are not supported by default. However, Coil has an extension library to support them.

To add GIF support, import the extension library:

```kotlin
implementation("io.coil-kt.coil3:coil-gif:3.2.0")
```

And that's it! The `ImageLoader` will automatically detect any GIFs using their file headers and decode them correctly.

Optionally, you can manually add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        if (SDK_INT >= 28) {
            add(AnimatedImageDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
    }
    .build()
```

To transform the pixel data of each frame of a GIF, see [AnimatedTransformation](/coil/api/coil-gif/coil3.gif/-animated-transformation).

!!! Note
    Coil includes two separate decoders to support decoding GIFs. `GifDecoder` supports all API levels, but is slower. `AnimatedImageDecoder` is powered by Android's [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder) API which is only available on API 28 and above. `AnimatedImageDecoder` is faster than `GifDecoder` and supports decoding animated WebP images and animated HEIF image sequences.
