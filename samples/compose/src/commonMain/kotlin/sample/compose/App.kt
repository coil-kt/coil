package sample.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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
        val showBackButton = screen is Screen.Detail || screen is Screen.Issue3260
        Scaffold(
            topBar = {
                Toolbar(
                    assetType = viewModel.assetType.collectAsState().value,
                    backEnabled = showBackButton,
                    onScreenChange = { viewModel.screen.value = it },
                    onAssetTypeChange = { viewModel.assetType.value = it },
                    onBackPressed = { viewModel.onBackPressed() },
                )
            },
            content = { padding ->
                ScaffoldContent(
                    screen = screen,
                    onScreenChange = { viewModel.screen.value = it },
                    images = viewModel.images.collectAsState().value,
                    padding = padding,
                )
            },
            modifier = Modifier.testTagsAsResourceId(true),
        )
        BackHandler(enabled = showBackButton) {
            viewModel.onBackPressed()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Toolbar(
    assetType: AssetType,
    backEnabled: Boolean,
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
                onClick = { onScreenChange(Screen.Issue3260) },
                content = { Text("BUG") },
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
    onScreenChange: (Screen) -> Unit,
    images: List<Image>,
    padding: PaddingValues,
) {
    // Reset the scroll position when the image list changes.
    // Preserve the scroll position when navigating to/from the detail screen.
    val gridState = rememberSaveable(images, saver = LazyStaggeredGridState.Saver) {
        LazyStaggeredGridState()
    }

    when (screen) {
        is Screen.Detail -> {
            DetailScreen(
                screen = screen,
                padding = padding,
            )
        }
        is Screen.Issue3260 -> {
            Issue3260Screen(padding = padding)
        }
        is Screen.List -> {
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
                )
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

/**
 * Reproduction screen for https://github.com/coil-kt/coil/issues/3260
 *
 * When using AsyncImage in a Column that's a sibling to another Column within a Row
 * with height(IntrinsicSize.Max), content below the AsyncImage fails to render.
 * The Text "This text should be visible!" is not rendered due to the bug.
 */
@Composable
private fun Issue3260Screen(padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
    ) {
        items(10) { index ->
            Column(
                modifier = Modifier
                    .fillParentMaxWidth(),
            ) {
                Text(
                    text = "Issue #3260: AsyncImage clips sibling content with IntrinsicSize.Max (Item ${index + 1})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Max)
                        .fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(16.dp),
                    ) {
                        Image(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                        )

                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .align(Alignment.CenterHorizontally)
                                .width(1.dp)
                        )
                    }
                    Column {
                        AsyncImage(
                            model = "https://picsum.photos/200/200",
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(100.dp),
                        )
                        Text(
                            text = "This text should be visible!",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }

                Text(
                    text = "If you see red text above saying 'This text should be visible!', the bug is fixed. " +
                        "If you only see the image with no text below it, the bug is present.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

const val Title = "Coil"

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
