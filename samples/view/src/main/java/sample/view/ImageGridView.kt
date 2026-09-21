package sample.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlin.math.roundToInt
import sample.common.MIN_COLUMN_WIDTH_DP

class ImageGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.recyclerview.R.attr.recyclerViewStyle,
) : RecyclerView(context, attrs, defStyleAttr) {

    private val gridLayoutManager = StaggeredGridLayoutManager(1, VERTICAL)

    init {
        layoutManager = gridLayoutManager
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthSpec) - paddingLeft - paddingRight
        val minColumnWidth =
            (MIN_COLUMN_WIDTH_DP * resources.displayMetrics.density).roundToInt().coerceAtLeast(1)
        val spanCount = (availableWidth / minColumnWidth).coerceAtLeast(1)
        if (gridLayoutManager.spanCount != spanCount) {
            gridLayoutManager.spanCount = spanCount
        }
        super.onMeasure(widthSpec, heightSpec)
    }
}
