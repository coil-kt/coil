package sample.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager.VERTICAL
import coil3.load
import kotlinx.coroutines.launch
import sample.common.AndroidMainViewModel
import sample.common.Image
import sample.common.NUM_COLUMNS
import sample.common.Screen
import sample.common.enableEdgeToEdge
import sample.common.extras
import sample.common.next
import sample.view.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val viewModel: AndroidMainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding
    private lateinit var listAdapter: ImageListAdapter
    private lateinit var backPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top
            }
            binding.list.updatePaddingRelative(bottom = navigationBars.bottom)
            binding.detail.updatePaddingRelative(bottom = navigationBars.bottom)
            binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = navigationBars.left
                rightMargin = navigationBars.right
            }

            insets
        }

        lifecycleScope.apply {
            launch { viewModel.assetType.collect { invalidateOptionsMenu() } }
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
                    extras(screen.image.extras)
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
