package coil.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import coil.compose.AsyncImage
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindowsCompat(false)
        setContent {
            MaterialTheme(
                colors = lightColors(
                    primary = Color.White,
                    onPrimary = Color.Black
                )
            ) {
                ProvideWindowInsets {
                    Scaffold(
                        topBar = {
                            Toolbar(viewModel.assetType)
                        },
                        content = {
                            Content(
                                assetTypeFlow = viewModel.assetType,
                                screenFlow = viewModel.screen,
                                imagesFlow = viewModel.images
                            )
                        }
                    )
                    BackHandler { viewModel.onBackPressed() }
                }
            }
        }
    }
}

@Composable
private fun Toolbar(assetType: MutableStateFlow<AssetType>) {
    val topPadding = with(LocalDensity.current) {
        LocalWindowInsets.current.systemBars.top.toDp()
    }
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = { AssetTypeButton(assetType) },
        modifier = Modifier.padding(top = topPadding)
    )
}

@Composable
private fun AssetTypeButton(assetTypeFlow: MutableStateFlow<AssetType>) {
    val assetType by assetTypeFlow.collectAsState()
    IconButton(
        onClick = { assetTypeFlow.value = assetType.next() }
    ) {
        Text(text = assetType.name)
    }
}

@Composable
private fun Content(
    assetTypeFlow: StateFlow<AssetType>,
    screenFlow: MutableStateFlow<Screen>,
    imagesFlow: StateFlow<List<Image>>
) {
    // Reset the scroll position when assetType changes.
    val assetType by assetTypeFlow.collectAsState()
    val listState = rememberSaveable(assetType, saver = LazyListState.Saver) { LazyListState() }
    when (val screen = screenFlow.collectAsState().value) {
        is Screen.Detail -> DetailScreen(screen)
        is Screen.List -> ListScreen(listState, screenFlow, imagesFlow)
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
    listState: LazyListState,
    screenFlow: MutableStateFlow<Screen>,
    imagesFlow: StateFlow<List<Image>>
) {
    val numColumns = numberOfColumns(LocalContext.current)
    val images by imagesFlow.collectAsState()

    // Migrate to LazyStaggeredVerticalGrid when it's implemented.
    LazyVerticalGrid(
        cells = GridCells.Fixed(numColumns),
        state = listState
    ) {
        items(images) { image ->
            // Scale the image to fit the width of a column.
            val size = with(LocalDensity.current) {
                image
                    .calculateScaledSize(LocalContext.current, numColumns)
                    .run { DpSize(width.toDp(), height.toDp()) }
            }

            // Intentionally not a `State` object to avoid recomposition.
            var placeholder: MemoryCache.Key? = null

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.uri)
                    .parameters(image.parameters)
                    .build(),
                contentDescription = null,
                placeholder = ColorPainter(Color(image.color)),
                error = ColorPainter(Color.Red),
                onSuccess = { placeholder = it.result.memoryCacheKey },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clickable { screenFlow.value = Screen.Detail(image, placeholder) }
            )
        }
    }
}
