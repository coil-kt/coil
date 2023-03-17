# Testing

To add testing support, import the extension library:

```kotlin
testImplementation("io.coil-kt:coil-test:2.3.0")
```

`coil-test` includes a `FakeImageLoaderEngine`, which can be added to your `ImageLoader` to intercept all incoming `ImageRequest`s and return a custom `ImageResult`. This is useful for testing as it makes loading images synchronous (from the main thread) and consistent. By using `FakeImageLoaderEngine` the `ImageLoader` will avoid all the memory caching, thread jumping, disk/network I/O fetching, and image decoding that's typically done to load an image. Here's an example:

```kotlin
val engine = FakeImageLoaderEngine()
engine.set("https://www.example.com/image.jpg", testDrawable)
engine.set({ it is String && it.endsWith("test.png") }, testDrawable)
engine.setFallback(ColorDrawable(Color.BLACK))

val imageLoader = ImageLoader.Builder(context)
    .components { add(engine) }
    .build()
Coil.setImageLoader(imageLoader)
```

This strategy works great with [Papparazzi](https://github.com/cashapp/paparazzi) to screenshot test UIs without a physical device or emulator:

```kotlin
class PaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi()

    @Before
    fun before() {
        val engine = FakeImageLoaderEngine()
        engine.setFallback(ColorDrawable(Color.RED))

        val imageLoader = ImageLoader.Builder(context)
            .components { add(engine) }
            .build()
        Coil.setImageLoader(imageLoader)
    }

    @Test
    fun testContentView() {
        val view: View = paparazzi.inflate(R.layout.content)
        paparazzi.snapshot(view)
    }

    @Test
    fun testContentCompose() {
        paparazzi.snapshot {
            Content()
        }
    }
}
```
