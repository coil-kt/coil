# Video Frames

To add video frame support, import the extension library:

```kotlin
implementation("io.coil-kt:coil-video:1.2.1")
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

`VideoFrameDecoder` handles all data sources, but creates a temporary file on disk to decode the source. `VideoFrameFileFetcher` and `VideoFrameUriFetcher` don't create a temporary file, but only work for `File`s and local `Uri`s respectively. Registering all 3 components ensures that `VideoFrameFileFetcher` and `VideoFrameUriFetcher` are automatically used when appropriate and `VideoFrameDecoder` is used as a fallback.

To specify the time code of the frame to extract from a video, use `videoFrameMillis` or `videoFrameMicros`:

```kotlin
imageView.load(File("/path/to/video.mp4")) {
    videoFrameMillis(1000)
}
```

If a frame time isn't specified, the first frame of the video is decoded.

The `ImageLoader` will automatically detect any videos and extract their frames if the request's filename/URI ends with a [valid video extension](https://developer.android.com/guide/topics/media/media-formats#video-formats). If it does not, you can [set the `Fetcher` explicitly](../api/coil-base/coil-base/coil.request/-image-request/-builder/fetcher.html) for the request.
