package coil.util

import android.app.Activity
import android.os.Bundle
import android.widget.ImageView
import coil.test.R

class TestActivity : Activity() {

    val imageView: ImageView by lazy { findViewById(R.id.image) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
    }
}
