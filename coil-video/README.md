# Video Frames

**This feature is only available on Android.**

To add video frame support, import the extension library:

```kotlin
implementation("io.coil-kt.coil3:coil-video:3.0.0-rc02")
```

And add the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(VideoFrameDecoder.Factory())
    }
    .build()
```

To specify the time of the frame to extract from a video, use `videoFrameMillis` or `videoFrameMicros`:

```kotlin
imageView.load("/path/to/video.mp4") {
    videoFrameMillis(1000)  // extracts the frame at 1 second of the video
}
```

For specifying the exact frame number, use `videoFrameIndex` (requires API level 28):

```kotlin
imageView.load("/path/to/video.mp4") {
    videoFrameIndex(1234)  // extracts the 1234th frame of the video
}
```

To select a video frame based on a percentage of the video's total duration, use `videoFramePercent`:

```kotlin
imageView.load("/path/to/video.mp4") {
    videoFramePercent(0.5)  // extracts the frame in the middle of the video's duration
}
```

If no frame position is specified, the first frame of the video will be decoded.

The `ImageLoader` will automatically detect any videos and extract their frames if the request's filename/URI ends with a [valid video extension](https://developer.android.com/guide/topics/media/media-formats#video-formats). If it does not, you can set the `Decoder` explicitly for the request:

```kotlin
imageView.load("/path/to/video") {
    decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
}
```
