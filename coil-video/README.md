# Video Frames

To add video frame support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-video:0.10.0-SNAPSHOT")
```

And add the two fetchers to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader(context) {
    componentRegistry {
        add(VideoFrameFileFetcher())
        add(VideoFrameUriFetcher())
    }
}
```

!!! Note
    Video frame decoding is only supported for `Uri`s (`content`, `file` schemes only) and `File`s.

To specify the time code of the frame to extract from a video, use `videoFrameMillis` or `videoFrameMicros`:

```kotlin
imageView.load(File("/path/to/video.mp4")) {
    videoFrameMillis(1000)
}
```

If a frame time isn't specified, the first frame of the video is decoded.

And that's it! The `ImageLoader` will automatically detect any videos and extract their frames.
