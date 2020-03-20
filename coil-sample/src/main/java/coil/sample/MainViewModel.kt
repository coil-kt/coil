package coil.sample

import android.app.Application
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import coil.fetch.VideoFrameFetcher.Companion.VIDEO_FRAME_MICROS_KEY
import coil.request.Parameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import org.json.JSONArray
import kotlin.random.Random

class MainViewModel(application: Application, handle: SavedStateHandle) : AndroidViewModel(application) {

    private val screenLiveData: MutableLiveData<Screen> = MutableLiveData(Screen.List) // Do not persist.
    private val imagesLiveData: MutableLiveData<List<Image>> = MutableLiveData(emptyList()) // Do not persist.
    private val assetTypeLiveData: MutableLiveData<AssetType> = handle.getLiveData("asset_type", AssetType.JPG)

    init {
        loadImages(assetTypeLiveData.requireValue())
    }

    fun screens(): LiveData<Screen> = screenLiveData

    fun setScreen(screen: Screen) {
        screenLiveData.postValue(screen)
    }

    fun assetTypes(): LiveData<AssetType> = assetTypeLiveData

    fun setAssetType(assetType: AssetType) {
        assetTypeLiveData.postValue(assetType)
        loadImages(assetType)
    }

    fun images(): LiveData<List<Image>> = imagesLiveData

    fun onBackPressed(): Boolean {
        return if (screenLiveData.value is Screen.Detail) {
            screenLiveData.value = Screen.List
            true
        } else {
            false
        }
    }

    private fun loadImages(assetType: AssetType) = viewModelScope.launch(Dispatchers.IO) {
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

        imagesLiveData.postValue(images)
    }
}
