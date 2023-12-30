package coil3.test.utils

import android.widget.ImageView
import androidx.activity.ComponentActivity

class ComposeTestActivity : ComponentActivity()

class ViewTestActivity : ComponentActivity(R.layout.activity_test) {
    val imageView: ImageView by lazy { findViewById(R.id.image) }
}
