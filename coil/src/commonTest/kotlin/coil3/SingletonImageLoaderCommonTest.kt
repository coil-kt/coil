package coil3

import coil3.test.utils.RobolectricTest
import coil3.test.utils.context
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.atomicfu.atomic

class SingletonImageLoaderCommonTest : RobolectricTest() {

    @AfterTest
    fun after() {
        SingletonImageLoader.reset()
    }

    @Test
    fun setImageLoaderFactoryIsInvokedExactlyOnce() {
        val imageLoader1 = ImageLoader(context)

        val factory = TestSingletonImageLoaderFactory(lazyOf(imageLoader1))
        SingletonImageLoader.setUnsafe(factory)

        assertFalse(factory.isInitialized)

        val imageLoader2 = SingletonImageLoader.get(context)

        assertSame(imageLoader1, imageLoader2)

        assertTrue(factory.isInitialized)

        val imageLoader3 = SingletonImageLoader.get(context)

        assertSame(imageLoader1, imageLoader3)
    }

    @Test
    fun setImageLoaderSafeThrowsIfDefaultIsAlreadyCreated() {
        SingletonImageLoader.get(context)

        assertFails {
            SingletonImageLoader.setSafe(::ImageLoader)
        }
    }
}

class TestSingletonImageLoaderFactory(
    private val imageLoaderLazy: Lazy<ImageLoader>,
) : SingletonImageLoader.Factory {

    constructor(context: PlatformContext) : this(lazy { ImageLoader(context) })

    private val _isInitialized = atomic(false)
    val isInitialized: Boolean by _isInitialized

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        check(!_isInitialized.getAndSet(true)) {
            "newImageLoader was invoked more than once."
        }
        return imageLoaderLazy.value
    }
}
