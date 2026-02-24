package sample.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.util.component1
import coil3.util.component2
import io.coil_kt.coil3.compose.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException
import sample.common.AssetType
import sample.common.Image
import sample.common.MainViewModel
import sample.common.NUM_COLUMNS
import sample.common.Resources
import sample.common.Screen
import sample.common.calculateScaledSize
import sample.common.extras
import sample.common.newImageLoader
import sample.common.next

@Composable
fun App() {
    val viewModel = remember {
        MainViewModel(ComposeResources())
    }
    LaunchedEffect(viewModel) {
        viewModel.start()
    }
    App(viewModel, debug = false)
}

@Composable
fun App(
    viewModel: MainViewModel,
    debug: Boolean,
) {
    setSingletonImageLoaderFactory { context ->
        newImageLoader(context, debug)
    }

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColors else lightColors,
    ) {
        val screen by viewModel.screen.collectAsState()
        var showDirectSvgScreen by rememberSaveable { mutableStateOf(false) }
        val isDetail = screen is Screen.Detail || showDirectSvgScreen
        Scaffold(
            topBar = {
                Toolbar(
                    assetType = viewModel.assetType.collectAsState().value,
                    backEnabled = isDetail,
                    onSvgClick = { showDirectSvgScreen = true },
                    onScreenChange = { viewModel.screen.value = it },
                    onAssetTypeChange = { viewModel.assetType.value = it },
                    onBackPressed = {
                        if (showDirectSvgScreen) {
                            showDirectSvgScreen = false
                        } else {
                            viewModel.onBackPressed()
                        }
                    },
                )
            },
            content = { padding ->
                ScaffoldContent(
                    screen = screen,
                    showDirectSvgScreen = showDirectSvgScreen,
                    onScreenChange = { viewModel.screen.value = it },
                    images = viewModel.images.collectAsState().value,
                    padding = padding,
                )
            },
            modifier = Modifier.testTagsAsResourceId(true),
        )
        BackHandler(enabled = isDetail) {
            if (showDirectSvgScreen) {
                showDirectSvgScreen = false
            } else {
                viewModel.onBackPressed()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    assetType: AssetType,
    backEnabled: Boolean,
    onSvgClick: () -> Unit,
    onScreenChange: (Screen) -> Unit,
    onAssetTypeChange: (AssetType) -> Unit,
    onBackPressed: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(Title)
        },
        navigationIcon = {
            if (backEnabled) {
                BackIconButton(onBackPressed)
            }
        },
        actions = {
            IconButton(
                onClick = onSvgClick,
                content = { Text("SVG") },
            )
            IconButton(
                onClick = { onScreenChange(resourceDetailScreen) },
                content = { Text("Res") },
            )
            IconButton(
                onClick = { onAssetTypeChange(assetType.next()) },
                content = { Text(assetType.name) },
            )
        },
        modifier = Modifier.statusBarsPadding(),
    )
}

@Composable
private fun BackIconButton(
    onBackPressed: () -> Unit,
) {
    IconButton(
        onClick = onBackPressed,
        content = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
            )
        },
    )
}

@Composable
private fun ScaffoldContent(
    screen: Screen,
    showDirectSvgScreen: Boolean,
    onScreenChange: (Screen) -> Unit,
    images: List<Image>,
    padding: PaddingValues,
) {
    // Reset the scroll position when the image list changes.
    // Preserve the scroll position when navigating to/from the detail screen.
    val gridState = rememberSaveable(images, saver = LazyStaggeredGridState.Saver) {
        LazyStaggeredGridState()
    }

    when {
        showDirectSvgScreen -> {
            DirectSvgScreen(
                padding = padding,
            )
        }
        screen is Screen.Detail -> {
            DetailScreen(
                screen = screen,
                padding = padding,
            )
        }
        screen is Screen.List -> {
            ListScreen(
                gridState = gridState,
                images = images,
                padding = padding,
                onImageClick = { image, placeholder ->
                    onScreenChange(Screen.Detail(image, placeholder))
                },
            )
        }
    }
}

@Composable
private fun DirectSvgScreen(
    padding: PaddingValues,
) {
    DirectSvgRenderer(
        svg = DirectSvgContent,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .testTag("direct_svg"),
    )
}

@Composable
private fun DetailScreen(
    screen: Screen.Detail,
    padding: PaddingValues,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(screen.image.uri)
            .placeholderMemoryCacheKey(screen.placeholder)
            .extras(screen.image.extras)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
    )
}

@Composable
private fun ListScreen(
    gridState: LazyStaggeredGridState,
    images: List<Image>,
    padding: PaddingValues,
    onImageClick: (Image, MemoryCache.Key?) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val screenWidth = containerSize().width

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(NUM_COLUMNS),
        state = gridState,
        contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
        modifier = Modifier
            .padding(
                PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection),
                ),
            )
            .testTag("list"),
    ) {
        items(
            items = images,
            key = { it.toString() },
        ) { image ->
            // Scale the image to fit the width of a column.
            val size = remember(density, screenWidth) {
                val (width, height) = image.calculateScaledSize(screenWidth)
                with(density) { DpSize(width.toDp(), height.toDp()) }
            }

            // Keep track of the image's memory cache key so it can be used as a placeholder
            // for the detail screen.
            var placeholder: MemoryCache.Key? = remember { null }

            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(image.uri)
                    .extras(image.extras)
                    .build(),
                contentDescription = null,
                placeholder = ColorPainter(Color(image.color)),
                error = ColorPainter(Color.Red),
                onSuccess = { placeholder = it.result.memoryCacheKey },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clickable { onImageClick(image, placeholder) },
            )
        }
    }
}

const val Title = "Coil"

private const val DirectSvgContent = """
    <svg xmlns="http://www.w3.org/2000/svg" width="640" height="240" viewBox="0 0 640 240">
      <rect width="640" height="240" fill="#f2f2f2"/>
      <text x="24" y="150" font-size="96" fill="#111111">Coil SVG Text</text>
    </svg>
"""

private val darkColors = darkColorScheme(
    background = Color(0xFF141218),
    surface = Color(0xFF141218),
)

private val lightColors = lightColorScheme(
    background = Color.White,
    surface = Color.White,
)

@OptIn(ExperimentalResourceApi::class)
private val resourceDetailScreen = Screen.Detail(
    image = Image(
        uri = Res.getUri("drawable/sample.jpg"),
        color = 0x00000000,
        width = 1024,
        height = 1326,
    ),
)

@OptIn(ExperimentalResourceApi::class)
private class ComposeResources : Resources {

    override fun uri(path: String): String {
        return try {
            Res.getUri("files/$path")
        } catch (_: MissingResourceException) {
            ""
        }
    }

    override suspend fun readBytes(path: String): ByteArray {
        return try {
            Res.readBytes("files/$path")
        } catch (_: MissingResourceException) {
            byteArrayOf()
        }
    }
}

@Stable
expect fun Modifier.testTagsAsResourceId(enable: Boolean): Modifier

@Composable
expect fun containerSize(): IntSize

@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
)

@Composable
expect fun DirectSvgRenderer(
    svg: String,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
)
