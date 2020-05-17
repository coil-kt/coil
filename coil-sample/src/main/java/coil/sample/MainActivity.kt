@file:Suppress("UNUSED_PARAMETER")

package coil.sample

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import coil.Coil
import coil.api.load
import coil.sample.databinding.ActivityMainBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var listAdapter: ImageListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        if (SDK_INT >= 29) {
            window.decorView.apply {
                systemUiVisibility = systemUiVisibility or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }
            binding.toolbar.setOnApplyWindowInsetsListener { view, insets ->
                view.updatePadding(top = insets.systemWindowInsetTop)
                insets
            }
        }

        listAdapter = ImageListAdapter(this) { viewModel.screen.value = it }
        binding.list.apply {
            setHasFixedSize(true)
            layoutManager = StaggeredGridLayoutManager(listAdapter.numColumns, VERTICAL)
            adapter = listAdapter
        }

        val imageLoader = Coil.imageLoader(this)
        imageLoader.apply {
            imageLoader.memoryCache.clear()
            imageLoader.bitmapPool.clear()
        }

        lifecycleScope.apply {
            launch {
                viewModel.assetType.collect { setAssetType(it) }
            }
            launch {
                viewModel.images.collect { setImages(it) }
            }
            launch {
                viewModel.screen.collect { setScreen(it) }
            }
        }
    }

    private fun setScreen(screen: Screen) {
        when (screen) {
            is Screen.List -> {
                binding.list.isVisible = true
                binding.detail.isVisible = false
            }
            is Screen.Detail -> {
                binding.list.isVisible = false
                binding.detail.isVisible = true
                binding.detail.load(screen.image.uri) {
                    parameters(screen.image.parameters)
                }
            }
        }
    }

    private fun setImages(images: List<Image>) {
        listAdapter.submitList(images) {
            // Ensure we're at the top of the list when the list items are updated.
            binding.list.scrollToPosition(0)
        }
    }

    private fun setAssetType(assetType: AssetType) {
        invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val title = viewModel.assetType.value.name
        val item = menu.add(Menu.NONE, R.id.action_toggle_asset_type, Menu.NONE, title)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_toggle_asset_type -> {
                val values = AssetType.values()
                val currentAssetType = viewModel.assetType.value
                val newAssetType = values[(values.indexOf(currentAssetType) + 1) % values.count()]
                viewModel.assetType.value = newAssetType
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
