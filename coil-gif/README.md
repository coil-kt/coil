# coil-gif

Adds GIF decoding support to Coil.

```kotlin
implementation("io.coil-kt:coil-gif:latest.version.here")
```

## Quick Start

Add the decoders to your component registry when constructing your ImageLoader:

```kotlin
// within a global place, like your Application.onCreate()
val imageLoader = ImageLoader(context) {
    componentRegistry {
        if (SDK_INT >= P) {
            add(ImageDecoderDecoder())
        } else {
            add(GifDecoder())
        }
    }
}
Coil.setDefaultImageLoader(imageLoader)
```

And that's it! The ImageLoader will automatically detect any GIFs using their file headers and decode them correctly.
