# Testing

To use the testing support classes, import the extension library:

```kotlin
testImplementation("io.coil-kt:coil-test:2.6.0")
```

`coil-test` includes a `FakeImageLoaderEngine`, which can be added to your `ImageLoader` to intercept all incoming `ImageRequest`s and return a custom `ImageResult`. This is useful for testing as it makes loading images synchronous (from the main thread) and consistent. By using `FakeImageLoaderEngine` the `ImageLoader` will avoid all the memory caching, thread jumping, disk/network I/O fetching, and image decoding that's typically done to load an image. Here's an example:

```kotlin
val engine = FakeImageLoaderEngine.Builder()
    .intercept("https://www.example.com/image.jpg", ColorDrawable(Color.RED))
    .intercept({ it is String && it.endsWith("test.png") }, ColorDrawable(Color.GREEN))
    .default(ColorDrawable(Color.BLUE))
    .build()
val imageLoader = ImageLoader.Builder(context)
    .components { add(engine) }
    .build()
Coil.setImageLoader(imageLoader)
```

This strategy works great with [Paparazzi](https://github.com/cashapp/paparazzi) to screenshot test UIs without a physical device or emulator:

```kotlin
class PaparazziTest {
    @get:Rule
    val paparazzi = Paparazzi()

    @Before
    fun before() {
        val engine = FakeImageLoaderEngine.Builder()
            .intercept("https://www.example.com/image.jpg", ColorDrawable(Color.RED))
            .intercept({ it is String && it.endsWith("test.png") }, ColorDrawable(Color.GREEN))
            .default(ColorDrawable(Color.BLUE))
            .build()
        val imageLoader = ImageLoader.Builder(paparazzi.context)
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
            AsyncImage(
                model = "https://www.example.com/image.jpg",
                contentDescription = null,
            )
        }
    }
}
```
