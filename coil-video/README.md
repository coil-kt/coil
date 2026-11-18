# Video Frames

**This feature is only available on Android.**

To add video frame support, import the extension library:

```kotlin
implementation("io.coil-kt.coil3:coil-video:3.6.3")
```

Coil registers `VideoFrameDecoder.Factory` automatically through its service loader. If you've disabled service loading, add the factory to your `ImageLoader`'s component registry:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .components {
        add(VideoFrameDecoder.Factory())
    }
    .build()
```

The decoder automatically handles sources whose MIME type starts with `video/`. Coil can infer this MIME type from a recognized file extension. If the source has a missing or incorrect MIME type, set the decoder explicitly for the request:

```kotlin
imageView.load("/path/to/video") {
    decoderFactory { result, options, _ -> VideoFrameDecoder(result.source, options) }
}
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
