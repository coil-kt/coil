package sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import sample.common.AndroidMainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AndroidMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(viewModel) }
    }
}
