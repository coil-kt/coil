package coil.sample

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import coil.sample.ImageListAdapter.ViewHolder
import kotlin.math.ceil
import kotlin.math.roundToInt

class ImageListAdapter(
    context: Context,
    private val setScreen: (Screen) -> Unit
) : ListAdapter<Image, ViewHolder>(Callback) {

    private val maxColumnWidth = 320.dp(context)
    private val displayWidth = context.getDisplaySize().width
    val numColumns = ceil(displayWidth / maxColumnWidth).toInt().coerceAtLeast(2)
    private val columnWidth = (displayWidth / numColumns.toDouble()).roundToInt()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.list_item))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.image.apply {
            val item = getItem(position)

            updateLayoutParams {
                val scale = columnWidth / item.width.toDouble()
                height = (scale * item.height).roundToInt()
                width = columnWidth
            }

            load(item.uri) {
                placeholder(ColorDrawable(item.color))
                parameters(item.parameters)
            }

            setOnClickListener {
                setScreen(Screen.Detail(item))
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image = itemView as ImageView
    }

    private object Callback : DiffUtil.ItemCallback<Image>() {
        override fun areItemsTheSame(old: Image, new: Image) = old.uri == new.uri
        override fun areContentsTheSame(old: Image, new: Image) = old == new
    }
}
