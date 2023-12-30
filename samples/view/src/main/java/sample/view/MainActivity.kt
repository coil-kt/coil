package sample.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import coil3.load
import kotlinx.coroutines.launch
import sample.common.AndroidMainViewModel
import sample.common.AssetType
import sample.common.Image
import sample.common.NUM_COLUMNS
import sample.common.Screen
import sample.common.next
import sample.view.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val viewModel: AndroidMainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var listAdapter: ImageListAdapter
    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        listAdapter = ImageListAdapter(
            resources = resources,
            setScreen = { viewModel.screen.value = it },
        )
        binding.list.apply {
            setHasFixedSize(true)
            layoutManager = StaggeredGridLayoutManager(NUM_COLUMNS, VERTICAL)
            adapter = listAdapter
        }

        backPressedCallback = onBackPressedDispatcher.addCallback(enabled = false) {
            viewModel.onBackPressed()
        }

        lifecycleScope.apply {
            launch { viewModel.assetType.collect(::setAssetType) }
            launch { viewModel.images.collect(::setImages) }
            launch { viewModel.screen.collect(::setScreen) }
        }
    }

    private fun setScreen(screen: Screen) {
        when (screen) {
            is Screen.List -> {
                backPressedCallback.isEnabled = false
                binding.list.isVisible = true
                binding.detail.isVisible = false
            }
            is Screen.Detail -> {
                backPressedCallback.isEnabled = true
                binding.list.isVisible = false
                binding.detail.isVisible = true
                binding.detail.load(screen.image.uri) {
                    placeholderMemoryCacheKey(screen.placeholder)
                    extras.setAll(screen.image.extras)
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

    @Suppress("UNUSED_PARAMETER")
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
                viewModel.assetType.value = viewModel.assetType.value.next()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}
