package coil.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.core.util.component1
import androidx.core.util.component2
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colors = lightColors(
                    primary = Color.White,
                    onPrimary = Color.Black
                )
            ) {
                Scaffold(
                    topBar = { Toolbar(viewModel) },
                    content = { Content(viewModel) }
                )
                BackHandler { viewModel.onBackPressed() }
            }
        }
    }
}

@Composable
private fun Toolbar(viewModel: MainViewModel) {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = { AssetTypeButton(viewModel) }
    )
}

@Composable
private fun AssetTypeButton(viewModel: MainViewModel) {
    val assetType by viewModel.assetType.collectAsState()
    IconButton(
        onClick = { viewModel.assetType.value = nextAssetType(assetType) }
    ) {
        Text(text = assetType.name)
    }
}

@Composable
private fun Content(viewModel: MainViewModel) {
    when (val screen = viewModel.screen.collectAsState().value) {
        is Screen.Detail -> DetailScreen(screen)
        is Screen.List -> ListScreen(viewModel)
    }
}

@Composable
private fun DetailScreen(screen: Screen.Detail) {
    val image = screen.image
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(image.uri)
            .parameters(image.parameters)
            .placeholderMemoryCacheKey(screen.placeholder)
            .build(),
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun ListScreen(viewModel: MainViewModel) {
    val numColumns = numberOfColumns(LocalContext.current)
    val images by viewModel.images.collectAsState()

    // TODO: Migrate to LazyStaggeredVerticalGrid when it's implemented.
    LazyVerticalGrid(
        cells = GridCells.Fixed(numColumns)
    ) {
        items(images) { image ->
            var placeholder: MemoryCache.Key? = null
            val (scaledWidth, scaledHeight) = scaledImageSize(
                context = LocalContext.current,
                numColumns = numColumns,
                width = image.width,
                height = image.height
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.uri)
                    .parameters(image.parameters)
                    .listener { _, result -> placeholder = result.memoryCacheKey }
                    .build(),
                contentDescription = null,
                modifier = with(LocalDensity.current) {
                    Modifier
                        .size(scaledWidth.toDp(), scaledHeight.toDp())
                        .clickable {
                            viewModel.screen.value = Screen.Detail(image, placeholder)
                        }
                }
            )
        }
    }
}
