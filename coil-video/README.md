# Video Frames

To add video frame support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-video:0.10.1")
```

And add the two fetchers to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .componentRegistry {
         add(VideoFrameFileFetcher())
         add(VideoFrameUriFetcher())
    }
    .build()
```

!!! Note
    Video frame decoding is only supported for `File`s and `Uri`s (`content` and `file` schemes only).

To specify the time code of the frame to extract from a video, use `videoFrameMillis` or `videoFrameMicros`:

```kotlin
imageView.load(File("/path/to/video.mp4")) {
    videoFrameMillis(1000)
}
```

If a frame time isn't specified, the first frame of the video is decoded.

And that's it! The `ImageLoader` will automatically detect any videos and extract their frames.
