package sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
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

@Preview
@Composable
fun Test() {
    AsyncImage(
        model = sample.common.R.drawable.ic_tinted_vector,
        contentDescription = null,
        modifier = Modifier.size(100.dp),
    )
}
