package coil.sample

import android.app.Application
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.fetch.VideoFrameFetcher.Companion.VIDEO_FRAME_MICROS_KEY
import coil.request.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import org.json.JSONArray
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _images: MutableStateFlow<List<Image>> = MutableStateFlow(emptyList())

    val assetType: MutableStateFlow<AssetType> = MutableStateFlow(AssetType.JPG)
    val screen: MutableStateFlow<Screen> = MutableStateFlow(Screen.List)
    val images: StateFlow<List<Image>> = _images

    init {
        viewModelScope.launch {
            assetType.collect { _images.value = loadImages(it) }
        }
    }

    fun onBackPressed(): Boolean {
        if (screen.value is Screen.Detail) {
            screen.value = Screen.List
            return true
        }
        return false
    }

    private suspend fun loadImages(assetType: AssetType): List<Image> = withContext(Dispatchers.IO) {
        val images = mutableListOf<Image>()

        if (assetType == AssetType.MP4) {
            for (index in 0 until 50) {
                val videoFrameMicros = Random.nextLong(62000000L)
                val parameters = Parameters.Builder()
                    .set(VIDEO_FRAME_MICROS_KEY, videoFrameMicros)
                    .build()

                images += Image(
                    uri = "file:///android_asset/${assetType.fileName}",
                    color = randomColor(),
                    width = 1280,
                    height = 720,
                    parameters = parameters
                )
            }
        } else {
            val json = JSONArray(context.assets.open(assetType.fileName).source().buffer().readUtf8())
            for (index in 0 until json.length()) {
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

                images += Image(
                    uri = url,
                    color = color,
                    width = image.getInt("width"),
                    height = image.getInt("height")
                )
            }
        }

        images
    }
}
