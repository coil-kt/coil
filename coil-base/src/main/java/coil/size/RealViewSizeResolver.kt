package coil.size

import android.view.View

internal class RealViewSizeResolver<T : View>(
    override val view: T,
    override val subtractPadding: Boolean
) : ViewSizeResolver<T> {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is RealViewSizeResolver<*> &&
            view == other.view &&
            subtractPadding == other.subtractPadding
    }

    override fun hashCode(): Int {
        var result = view.hashCode()
        result = 31 * result + subtractPadding.hashCode()
        return result
    }

    override fun toString(): String {
        return "RealViewSizeResolver(view=$view, subtractPadding=$subtractPadding)"
    }
}
