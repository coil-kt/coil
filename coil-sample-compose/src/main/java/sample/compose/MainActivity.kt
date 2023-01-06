package sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.DpSize
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest
import sample.common.AssetType
import sample.common.Image
import sample.common.MainViewModel
import sample.common.Screen
import sample.common.calculateScaledSize
import sample.common.next
import sample.common.numberOfColumns

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { Content() }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Content(viewModel: MainViewModel = viewModel()) {
    MaterialTheme(
        colors = lightColors(
            primary = Color.White,
            onPrimary = Color.Black
        )
    ) {
        val screen by viewModel.screen.collectAsState()
        Scaffold(
            topBar = {
                Toolbar(
                    assetType = viewModel.assetType.collectAsState().value,
                    onAssetTypeChange = { viewModel.assetType.value = it }
                )
            },
            content = { padding ->
                Box(Modifier.padding(padding)) {
                    ScaffoldContent(
                        screen = screen,
                        onScreenChange = { viewModel.screen.value = it },
                        images = viewModel.images.collectAsState().value
                    )
                }
            },
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            }
        )
        BackHandler(enabled = screen is Screen.Detail) {
            viewModel.onBackPressed()
        }
    }
}

@Composable
private fun Toolbar(
    assetType: AssetType,
    onAssetTypeChange: (AssetType) -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = { AssetTypeButton(assetType, onAssetTypeChange) },
        modifier = Modifier.statusBarsPadding()
    )
}

@Composable
private fun AssetTypeButton(
    assetType: AssetType,
    onAssetTypeChange: (AssetType) -> Unit,
) {
    IconButton(
        onClick = { onAssetTypeChange(assetType.next()) },
        content = { Text(assetType.name) }
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ScaffoldContent(
    screen: Screen,
    onScreenChange: (Screen) -> Unit,
    images: List<Image>,
) {
    // Reset the scroll position when the image list changes.
    // Preserve the scroll position when navigating to/from the detail screen.
    val gridState = rememberSaveable(images, saver = LazyStaggeredGridState.Saver) {
        LazyStaggeredGridState()
    }

    when (screen) {
        is Screen.Detail -> {
            DetailScreen(screen)
        }
        is Screen.List -> {
            ListScreen(
                gridState = gridState,
                images = images,
                onImageClick = { image, placeholder ->
                    onScreenChange(Screen.Detail(image, placeholder))
                }
            )
        }
    }
}

@Composable
private fun DetailScreen(screen: Screen.Detail) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(screen.image.uri)
            .parameters(screen.image.parameters)
            .placeholderMemoryCacheKey(screen.placeholder)
            .build(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ListScreen(
    gridState: LazyStaggeredGridState,
    images: List<Image>,
    onImageClick: (Image, MemoryCache.Key?) -> Unit,
) {
    val context = LocalContext.current
    val numColumns = remember(context) { numberOfColumns(context) }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(numColumns),
        state = gridState,
        modifier = Modifier.testTag("list"),
    ) {
        items(images) { image ->
            // Scale the image to fit the width of a column.
            val size = with(LocalDensity.current) {
                image
                    .calculateScaledSize(LocalContext.current, numColumns)
                    .run { DpSize(width.toDp(), height.toDp()) }
            }

            // Intentionally not a state object to avoid recomposition.
            var placeholder: MemoryCache.Key? = null

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.uri)
                    .parameters(image.parameters)
                    .build(),
                placeholder = ColorPainter(Color(image.color)),
                error = ColorPainter(Color.Red),
                onSuccess = { placeholder = it.result.memoryCacheKey },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clickable { onImageClick(image, placeholder) }
            )
        }
    }
}
