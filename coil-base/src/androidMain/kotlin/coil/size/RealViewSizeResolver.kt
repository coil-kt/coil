package coil.size

import android.view.View
import dev.drewhamilton.poko.Poko

@Poko
internal class RealViewSizeResolver<T : View>(
    override val view: T,
    override val subtractPadding: Boolean,
) : ViewSizeResolver<T>
