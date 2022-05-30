package coil.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { Content() }
    }
}

@Composable
private fun Content(viewModel: MainViewModel = viewModel()) {
    MaterialTheme(
        colors = lightColors(
            primary = Color.White,
            onPrimary = Color.Black
        )
    ) {
        ProvideWindowInsets {
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
                            screen = viewModel.screen.collectAsState().value,
                            onScreenChange = { viewModel.screen.value = it },
                            images = viewModel.images.collectAsState().value
                        )
                    }
                }
            )
            BackHandler { viewModel.onBackPressed() }
        }
    }
}

@Composable
private fun Toolbar(
    assetType: AssetType,
    onAssetTypeChange: (AssetType) -> Unit,
) {
    val topPadding = with(LocalDensity.current) {
        LocalWindowInsets.current.systemBars.top.toDp()
    }
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = { AssetTypeButton(assetType, onAssetTypeChange) },
        modifier = Modifier.padding(top = topPadding)
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
private fun ScaffoldContent(
    screen: Screen,
    onScreenChange: (Screen) -> Unit,
    images: List<Image>,
) {
    val context = LocalContext.current
    val numColumns = remember(context) { numberOfColumns(context) }
    val listStates = List(numColumns) {
        // Reset the scroll position when the image list changes.
        rememberSaveable(images, saver = LazyListState.Saver) { LazyListState() }
    }

    when (screen) {
        is Screen.Detail -> {
            DetailScreen(screen)
        }
        is Screen.List -> {
            ListScreen(
                listStates = listStates,
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
private fun ListScreen(
    listStates: List<LazyListState>,
    images: List<Image>,
    onImageClick: (Image, MemoryCache.Key?) -> Unit,
) {
    LazyStaggeredGrid(
        columnCount = listStates.size,
        states = listStates
    ) {
        items(images) { image ->
            // Scale the image to fit the width of a column.
            val size = with(LocalDensity.current) {
                image
                    .calculateScaledSize(LocalContext.current, listStates.size)
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
