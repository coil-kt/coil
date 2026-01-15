package sample.common

import coil3.memory.MemoryCache

sealed interface Screen {

    data object List : Screen

    data class Detail(
        val image: Image,
        val placeholder: MemoryCache.Key? = null,
    ) : Screen

    // Reproduction screen for https://github.com/coil-kt/coil/issues/3260
    data object Issue3260 : Screen
}
