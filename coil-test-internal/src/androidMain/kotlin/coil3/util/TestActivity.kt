package coil3.util

import android.widget.ImageView
import androidx.activity.ComponentActivity
import coil3.test.internal.R

class ComposeTestActivity : ComponentActivity()

class ViewTestActivity : ComponentActivity(R.layout.activity_test) {
    val imageView: ImageView by lazy { findViewById(R.id.image) }
}
