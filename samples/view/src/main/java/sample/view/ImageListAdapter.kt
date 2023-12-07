package sample.view

import android.content.res.Resources as AndroidResources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.memory.MemoryCache
import coil3.request.error
import coil3.request.placeholder
import sample.common.Image
import sample.common.Screen
import sample.common.calculateScaledSize
import sample.view.ImageListAdapter.ViewHolder

class ImageListAdapter(
    private val resources: AndroidResources,
    private val setScreen: (Screen) -> Unit
) : ListAdapter<Image, ViewHolder>(Callback.asConfig()) {

    private val displayWidth = resources.displayMetrics.widthPixels

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.list_item))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.image.apply {
            val item = getItem(position)

            updateLayoutParams {
                val (width, height) = item.calculateScaledSize(displayWidth)
                this.width = width
                this.height = height
            }

            var placeholder: MemoryCache.Key? = null

            load(item.uri) {
                placeholder(ColorDrawable(item.color))
                error(ColorDrawable(Color.RED))
                extras.setAll(item.extras)
                listener { _, result -> placeholder = result.memoryCacheKey }
            }

            setOnClickListener {
                setScreen(Screen.Detail(item, placeholder))
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
