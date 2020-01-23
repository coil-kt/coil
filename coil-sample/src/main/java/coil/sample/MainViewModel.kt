package coil.sample

import android.app.Application
import android.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import org.json.JSONArray
import kotlin.random.Random

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val screenLiveData = MutableLiveData<Screen>(Screen.List)
    private val imagesLiveData = MutableLiveData<List<Image>>()
    private val assetTypeLiveData = MutableLiveData<AssetType>()

    init {
        setAssetType(AssetType.JPG)
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
        val json = JSONArray(context.assets.open(assetType.fileName).source().buffer().readUtf8())

        val images = mutableListOf<Image>()
        for (index in 0 until json.length()) {
            val image = json.getJSONObject(index)

            val url: String
            val color: Int
            if (assetType == AssetType.JPG) {
                url = image.getJSONObject("urls").getString("regular")
                color = image.getString("color").toColorInt()
            } else {
                url = image.getString("url")
                color = Color.argb(128, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
            }

            images += Image(
                url = url,
                color = color,
                width = image.getInt("width"),
                height = image.getInt("height")
            )
        }

        imagesLiveData.postValue(images)
    }
}
