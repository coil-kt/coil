# Video Frames

To add video frame support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-video:1.3.0")
```

And add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(VideoFrameDecoder.Factory())
    }
    .build()
```

To specify the time code of the frame to extract from a video, use `videoFrameMillis` or `videoFrameMicros`:

```kotlin
imageView.load(File("/path/to/video.mp4")) {
    videoFrameMillis(1000)
}
```

If a frame time isn't specified, the first frame of the video is decoded.

The `ImageLoader` will automatically detect any videos and extract their frames if the request's filename/URI ends with a [valid video extension](https://developer.android.com/guide/topics/media/media-formats#video-formats). If it does not, you can [set the `Decoder` explicitly](../api/coil-base/coil.request/-image-request/-builder/decoder-factory.html) for the request.
