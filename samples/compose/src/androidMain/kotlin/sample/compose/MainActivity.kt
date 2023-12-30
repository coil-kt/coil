package sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import sample.common.AndroidMainViewModel
import sample.common.BuildConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App(
                viewModel = viewModel<AndroidMainViewModel>(),
                debug = BuildConfig.DEBUG,
            )
        }
    }
}
