package coil.util

import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.base.test.R

class TestActivity : AppCompatActivity(R.layout.ic_test_activity) {

    val imageView: ImageView by lazy { findViewById(R.id.image) }
}
