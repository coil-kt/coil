# Video Frames

To add video frame support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-video:0.8.0-SNAPSHOT")
```

And add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader(context) {
    componentRegistry {
        add(VideoFrameDecoder())
    }
}
```

To specify the time code of the frame to extract from a video, use `videoFrameMillis` or `videoFrameMicros`:

```kotlin
imageView.load("https://www.example.com/video.mp4") {
    videoFrameMillis(1000)
}
```

And that's it! The `ImageLoader` will automatically detect any videos and extract their frames them correctly.
