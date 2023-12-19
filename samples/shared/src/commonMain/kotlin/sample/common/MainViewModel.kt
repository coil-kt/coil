package sample.common

import coil3.Extras
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.buffer
import okio.use

fun MainViewModel(resources: Resources): MainViewModel {
    return RealMainViewModel(resources)
}

interface MainViewModel {
    val images: StateFlow<List<Image>>
    val assetType: MutableStateFlow<AssetType>
    val screen: MutableStateFlow<Screen>

    suspend fun start()
    fun onBackPressed()
}

private class RealMainViewModel(
    private val resources: Resources,
) : MainViewModel {

    private val _images: MutableStateFlow<List<Image>> = MutableStateFlow(emptyList())
    override val images: StateFlow<List<Image>> get() = _images
    override val assetType: MutableStateFlow<AssetType> = MutableStateFlow(AssetType.JPG)
    override val screen: MutableStateFlow<Screen> = MutableStateFlow(Screen.List)

    override suspend fun start() {
        assetType.collect { assetType ->
            try {
                _images.value = loadImagesAsync(assetType)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onBackPressed() {
        // Always navigate to the top-level list if this method is called.
        screen.value = Screen.List
    }

    private suspend fun loadImagesAsync(assetType: AssetType) = withContext(Dispatchers.Default) {
        if (assetType == AssetType.MP4) {
            loadVideoFrames()
        } else {
            loadImages(assetType)
        }
    }

    private fun loadVideoFrames(): List<Image> {
        return List(200) {
            val videoFrameMicros = Random.nextLong(62_000_000L)
            val extras = Extras.Builder()
                .set(Extras.Key.videoFrameMicros, videoFrameMicros)
                .build()

            Image(
                uri = "${resources.root}/${AssetType.MP4.fileName}",
                color = randomColor(),
                width = 1280,
                height = 720,
                extras = extras,
            )
        }
    }

    private suspend fun loadImages(assetType: AssetType): List<Image> {
        val json = resources.open(assetType.fileName).buffer().use {
            Json.parseToJsonElement(it.readUtf8()).jsonArray
        }
        return List(json.size) { index ->
            val image = json[index].jsonObject

            val url: String
            val color: Int
            if (assetType == AssetType.JPG) {
                url = image.getValue("urls").jsonObject.getValue("regular").jsonPrimitive.content
                color = image.getValue("color").jsonPrimitive.content.toColorInt()
            } else {
                url = image.getValue("url").jsonPrimitive.content
                color = randomColor()
            }

            Image(
                uri = url,
                color = color,
                width = image.getValue("width").jsonPrimitive.content.toInt(),
                height = image.getValue("height").jsonPrimitive.content.toInt()
            )
        }
    }
}
