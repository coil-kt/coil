package coil.sample

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.SuccessResult
import coil.result
import coil.sample.ImageListAdapter.ViewHolder

class ImageListAdapter(
    private val numColumns: Int,
    private val setScreen: (Screen) -> Unit
) : ListAdapter<Image, ViewHolder>(Callback.asConfig()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.list_item))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.image.apply {
            val item = getItem(position)

            updateLayoutParams {
                val size = scaledImageSize(context, numColumns, item.width, item.height)
                height = size.width
                width = size.height
            }

            load(item.uri) {
                placeholder(ColorDrawable(item.color))
                error(ColorDrawable(Color.RED))
                parameters(item.parameters)
            }

            setOnClickListener {
                setScreen(Screen.Detail(item, (result as? SuccessResult)?.memoryCacheKey))
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image get() = itemView as ImageView
    }

    private object Callback : DiffUtil.ItemCallback<Image>() {
        override fun areItemsTheSame(old: Image, new: Image) = old.uri == new.uri
        override fun areContentsTheSame(old: Image, new: Image) = old == new
    }
}
