# Testing

To use the testing support classes, import the extension library:

```kotlin
testImplementation("io.coil-kt.coil3:coil-test:3.3.0")
```

`coil-test` includes a `FakeImageLoaderEngine`, which can be added to your `ImageLoader` to intercept all incoming `ImageRequest`s and return a custom `ImageResult`. This is useful for testing as it makes loading images synchronous (from the main thread) and consistent. By using `FakeImageLoaderEngine` the `ImageLoader` will avoid all the memory caching, thread jumping, disk/network I/O fetching, and image decoding that's typically done to load an image. Here's an example:

```kotlin
val engine = FakeImageLoaderEngine.Builder()
    .intercept("https://example.com/image.jpg", ColorImage(Color.Red.toArgb()))
    .intercept({ it is String && it.endsWith("test.png") }, ColorImage(Color.Green.toArgb()))
    .default(ColorImage(Color.Blue.toArgb()))
    .build()
val imageLoader = ImageLoader.Builder(context)
    .components { add(engine) }
    .build()
```

`ColorImage` is useful for testing as it can draw a colored box or fill the canvas with a color depending on its width/height and it is supported on all platforms.

This strategy works great with [Paparazzi](https://github.com/cashapp/paparazzi) to screenshot test UIs without a physical device or emulator:

```kotlin
class PaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi()

    @Before
    fun before() {
        val engine = FakeImageLoaderEngine.Builder()
            .intercept("https://example.com/image.jpg", ColorImage(Color.Red.toArgb()))
            .intercept({ it is String && it.endsWith("test.png") }, ColorImage(Color.Green.toArgb()))
            .default(ColorImage(Color.Blue.toArgb()))
            .build()
        val imageLoader = ImageLoader.Builder(paparazzi.context)
            .components { add(engine) }
            .build()
        SingletonImageLoader.setUnsafe(imageLoader)
    }

    @Test
    fun testContentComposeRed() {
        // Will display a red box.
        paparazzi.snapshot {
            AsyncImage(
                model = "https://example.com/image.jpg",
                contentDescription = null,
            )
        }
    }

    @Test
    fun testContentComposeGreen() {
        // Will display a green box.
        paparazzi.snapshot {
            AsyncImage(
                model = "https://www.example.com/test.png",
                contentDescription = null,
            )
        }
    }

    @Test
    fun testContentComposeBlue() {
        // Will display a blue box.
        paparazzi.snapshot {
            AsyncImage(
                model = "https://www.example.com/default.png",
                contentDescription = null,
            )
        }
    }
}
```
