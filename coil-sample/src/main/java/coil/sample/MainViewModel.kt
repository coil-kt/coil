package coil.sample

import android.app.Application
import android.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okio.buffer
import okio.source
import org.json.JSONArray

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val screenLiveData = MutableLiveData<Screen>().apply { value = Screen.List }
    val imagesLiveData = MutableLiveData<List<Image>>()

    init {
        loadImages()
    }

    fun onBackPressed(): Boolean {
        return if (screenLiveData.value is Screen.Detail) {
            screenLiveData.value = Screen.List
            true
        } else {
            false
        }
    }

    private fun loadImages() = scope.launch(Dispatchers.IO) {
        val json = JSONArray(context.assets.open("images.json").source().buffer().readUtf8())

        val images = mutableListOf<Image>()
        for (index in 0 until json.length()) {
            val image = json.getJSONObject(index)
            images += Image(
                url = image.getJSONObject("urls").getString("regular"),
                color = Color.parseColor(image.getString("color")),
                width = image.getInt("width"),
                height = image.getInt("height")
            )
        }

        imagesLiveData.postValue(images)
    }

    override fun onCleared() {
        scope.cancel()
    }
}
