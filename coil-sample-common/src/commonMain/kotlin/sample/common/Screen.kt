package sample.common

import coil3.memory.MemoryCache

sealed interface Screen {

    data object List : Screen

    data class Detail(
        val image: Image,
        val placeholder: MemoryCache.Key?,
    ) : Screen
}
