package sample.common

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AndroidMainViewModel(
    application: Application,
) : AndroidViewModel(application), MainViewModel by MainViewModel(AndroidResources(application)) {
    init {
        viewModelScope.launch { start() }
    }
}
