package coil3.compose.internal

import kotlin.coroutines.CoroutineContext

/**
 * A special [CoroutineContext] implementation that observes changes to its elements.
 */
internal abstract class ForwardingCoroutineContext(
    private val delegate: CoroutineContext,
) : CoroutineContext by delegate {

    abstract fun newContext(
        old: CoroutineContext,
        new: CoroutineContext,
    ): ForwardingCoroutineContext

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        val new = delegate.minusKey(key)
        return newContext(this, new)
    }

    override operator fun plus(context: CoroutineContext): CoroutineContext {
        val new = delegate + context
        return newContext(this, new)
    }

    override fun equals(other: Any?): Boolean {
        return delegate == other
    }

    override fun hashCode(): Int {
        return delegate.hashCode()
    }

    override fun toString(): String {
        return "ForwardingCoroutineContext(delegate=$delegate)"
    }
}
