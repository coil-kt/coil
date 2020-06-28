package coil.sample

import coil.annotation.ExperimentalCoilApi
import coil.memory.MemoryCache

sealed class Screen {

    object List : Screen()

    @OptIn(ExperimentalCoilApi::class)
    data class Detail(val image: Image, val placeholderKey: MemoryCache.Key?) : Screen()
}
