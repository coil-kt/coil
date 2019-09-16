package coil.sample

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.api.load
import coil.transform.RoundedCornersTransformation

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by bindViewModel()

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val list: RecyclerView by bindView(R.id.list)
    private val detail: ImageView by bindView(R.id.detail)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val listAdapter = ImageListAdapter(this, viewModel.screenLiveData::setValue)
        list.apply {
            setHasFixedSize(true)
            layoutManager = StaggeredGridLayoutManager(
                listAdapter.numColumns,
                StaggeredGridLayoutManager.VERTICAL
            )
            adapter = listAdapter
        }

        viewModel.screenLiveData.observe(this, Observer(::setScreen))
        viewModel.imagesLiveData.observe(this, Observer(listAdapter::submitList))
    }

    private fun setScreen(screen: Screen) {
        when (screen) {
            is Screen.List -> {
                list.isVisible = true
                detail.isVisible = false
            }
            is Screen.Detail -> {
                list.isVisible = false
                detail.isVisible = true
                detail.load(screen.image.url) {
                    transformations(RoundedCornersTransformation(20f, 10f, 50f, 40f))
                    placeholder(ColorDrawable(screen.image.color))
                }
            }
        }
    }

    override fun onBackPressed() {
        if (!viewModel.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
