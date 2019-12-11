package coil.sample

import android.graphics.drawable.ColorDrawable
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.Q
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.api.load
import coil.request.CachePolicy

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val toolbar: Toolbar by bindView(R.id.toolbar)
    private val list: RecyclerView by bindView(R.id.list)
    private val detail: ImageView by bindView(R.id.detail)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if (SDK_INT >= Q) {
            window.decorView.apply {
                systemUiVisibility = systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }
            toolbar.setOnApplyWindowInsetsListener { view, insets ->
                view.updatePadding(top = insets.systemWindowInsetTop)
                insets
            }
        }

        val listAdapter = ImageListAdapter(this, viewModel::setScreen)
        list.apply {
            setHasFixedSize(true)
            layoutManager = StaggeredGridLayoutManager(listAdapter.numColumns, StaggeredGridLayoutManager.VERTICAL)
            adapter = listAdapter
        }

        viewModel.screens().observe(this, Observer(::setScreen))
        viewModel.images().observe(this, Observer(listAdapter::submitList))
        viewModel.assetTypes().observe(this, Observer { invalidateOptionsMenu() })
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
                    placeholder(ColorDrawable(screen.image.color))
                    memoryCachePolicy(CachePolicy.READ_ONLY)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val title = viewModel.assetTypes().requireValue().name
        val item = menu.add(Menu.NONE, R.id.action_toggle_asset_type, Menu.NONE, title)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_toggle_asset_type -> {
                val values = AssetType.values()
                val currentAssetType = viewModel.assetTypes().requireValue()
                val newAssetType = values[(values.indexOf(currentAssetType) + 1) % values.count()]
                viewModel.setAssetType(newAssetType)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed() {
        if (!viewModel.onBackPressed()) {
            super.onBackPressed()
        }
    }
}
