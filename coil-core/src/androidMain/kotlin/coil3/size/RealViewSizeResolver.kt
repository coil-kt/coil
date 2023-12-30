package coil3.size

import android.view.View
import coil3.annotation.Data

@Data
internal class RealViewSizeResolver<T : View>(
    override val view: T,
    override val subtractPadding: Boolean,
) : ViewSizeResolver<T>
