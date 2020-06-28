package coil.sample

import coil.memory.MemoryCache

sealed class Screen {

    object List : Screen()

    data class Detail(val image: Image, val placeholderKey: MemoryCache.Key?) : Screen()
}
