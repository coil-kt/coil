package coil.util

import android.widget.ImageView
import androidx.activity.ComponentActivity
import coil.test.R

class ComposeTestActivity : ComponentActivity()

class ViewTestActivity : ComponentActivity(R.layout.activity_test) {
    val imageView: ImageView by lazy { findViewById(R.id.image) }
}
