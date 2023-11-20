package sample.common

import android.app.Application
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.Extras
import coil.request.videoFrameMicros
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import org.json.JSONArray

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _images: MutableStateFlow<List<Image>> = MutableStateFlow(emptyList())
    val images: StateFlow<List<Image>> get() = _images

    val assetType: MutableStateFlow<AssetType> = MutableStateFlow(AssetType.JPG)
    val screen: MutableStateFlow<Screen> = MutableStateFlow(Screen.List)

    init {
        viewModelScope.launch {
            assetType.collect { _images.value = loadImagesAsync(it) }
        }
    }

    fun onBackPressed() {
        // Always navigate to the top-level list if this method is called.
        screen.value = Screen.List
    }

    private suspend fun loadImagesAsync(assetType: AssetType) = withContext(Dispatchers.IO) {
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
                uri = "file:///android_asset/${AssetType.MP4.fileName}",
                color = randomColor(),
                width = 1280,
                height = 720,
                extras = extras,
            )
        }
    }

    private fun loadImages(assetType: AssetType): List<Image> {
        val json = context.assets.open(assetType.fileName).source().buffer().use {
            JSONArray(it.readUtf8())
        }
        return List(json.length()) { index ->
            val image = json.getJSONObject(index)

            val url: String
            val color: Int
            if (assetType == AssetType.JPG) {
                url = image.getJSONObject("urls").getString("regular")
                color = image.getString("color").toColorInt()
            } else {
                url = image.getString("url")
                color = randomColor()
            }

            Image(
                uri = url,
                color = color,
                width = image.getInt("width"),
                height = image.getInt("height")
            )
        }
    }
}
