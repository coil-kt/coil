# GIFs

Unlike Glide, GIFs are not supported by default. However, Coil has an extension library to support them.

To add GIF support, import the extension library:

```kotlin
implementation("io.coil-kt.coil3:coil-gif:3.0.1")
```

And that's it! The `ImageLoader` will automatically detect any supported files using their headers and decode them correctly.

Optionally, you can manually add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
// For Android
val imageLoader = ImageLoader.Builder(context)
    .components {
        if (SDK_INT >= 28) {
            add(AnimatedImageDecoder.Factory())
        } else {
            add(GifDecoder.Factory())
        }
    }
    .build()

// For other platforms
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(AnimatedSkiaImageDecoder.Factory())
    }
    .build()
```

## Supported decoders and formats

| Decoder                    | Supported Platforms | Supported Formats | Notes                                                                                                           |
|----------------------------|---------------------|-------------------|-----------------------------------------------------------------------------------------------------------------|
| `AnimatedImageDecoder`     | Android (API 28+)   | GIF, WebP, HEIF   | Powered by Android's [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder) API. |
| `GifDecoder`               | Android             | GIF               | Slower than `AnimatedImageDecoder`.                                                                             |
| `AnimatedSkiaImageDecoder` | Other platforms     | GIF, WebP         |                                                                                                                 |

## Transforms

On Android, to transform the pixel data of each frame of a GIF, see [AnimatedTransformation](/coil/api/coil-gif/coil3.gif/-animated-transformation).
