package coil3.compose

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.internal.toCoilSizeOrNull
import coil3.size.Size as CoilSize
import coil3.size.SizeResolver
import kotlin.js.JsName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transformLatest

@ExperimentalCoilApi
@JsName("newDrawScopeSizeResolver")
fun DrawScopeSizeResolver(): DrawScopeSizeResolver = RealDrawScopeSizeResolver()

/**
 * A special [SizeResolver] that waits until [AsyncImagePainter.onDraw] to return the
 * [DrawScope]'s size.
 */
@ExperimentalCoilApi
interface DrawScopeSizeResolver : SizeResolver {
    fun connect(sizes: Flow<Size>)
}

private class RealDrawScopeSizeResolver : DrawScopeSizeResolver {
    private val sizes = MutableSharedFlow<Flow<Size>>(
        replay = 1,
        onBufferOverflow = DROP_OLDEST,
    )

    override fun connect(sizes: Flow<Size>) {
        this.sizes.tryEmit(sizes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun size(): CoilSize {
        return sizes
            .transformLatest { emitAll(it) }
            .mapNotNull { it.toCoilSizeOrNull() }
            .first()
    }
}
