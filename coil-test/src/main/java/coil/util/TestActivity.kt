package coil.util

import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.test.R

class TestActivity : AppCompatActivity(R.layout.activity_test) {

    val imageView: ImageView by lazy { findViewById(R.id.image) }
}
