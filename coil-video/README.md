# Video Frames

To add video frame support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-video:1.1.1")
```

And add the two fetchers and the decoder to your component registry when constructing your `ImageLoader`:

```kotlin
val imageLoader = ImageLoader.Builder(context)
    .componentRegistry {
        add(VideoFrameFileFetcher())
        add(VideoFrameUriFetcher())
        add(VideoFrameDecoder())
    }
    .build()
```

`VideoFrameFileFetcher` and `VideoFrameUriFetcher` are the optimal ways to decode video frames from `File`s and non-network `Uri`s and will be used automatically for those data types. `VideoFrameDecoder` handles all data types, but creates a temporary file on disk to decode the source.

To specify the time code of the frame to extract from a video, use `videoFrameMillis` or `videoFrameMicros`:

```kotlin
imageView.load(File("/path/to/video.mp4")) {
    videoFrameMillis(1000)
}
```

If a frame time isn't specified, the first frame of the video is decoded.

The `ImageLoader` will automatically detect any videos and extract their frames if the request's filename/URI ends with a [valid video extension](https://developer.android.com/guide/topics/media/media-formats#video-formats). If it does not, you can [set the `Fetcher` explicitly](../api/coil-base/coil.request/-image-request/-builder/fetcher/) for the request.
