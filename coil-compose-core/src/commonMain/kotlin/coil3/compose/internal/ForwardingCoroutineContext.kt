package coil3.compose.internal

import kotlin.coroutines.CoroutineContext

internal class ForwardingCoroutineContext(
    private val delegate: CoroutineContext,
    private val onNewContext: (old: CoroutineContext, new: CoroutineContext) -> Unit,
) : CoroutineContext by delegate {

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        val new = delegate.minusKey(key)
        onNewContext(this, new)
        return ForwardingCoroutineContext(new, onNewContext)
    }

    override operator fun plus(context: CoroutineContext): CoroutineContext {
        val new = delegate + context
        onNewContext(this, new)
        return ForwardingCoroutineContext(new, onNewContext)
    }

    override fun equals(other: Any?): Boolean {
        return delegate == other
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun toString(): String {
        return delegate.toString()
    }
}
