package coil.fetch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import coil.bitmap.BitmapPool
import org.junit.Before

class PdfPageFetcherTest {

    private lateinit var context: Context
    private lateinit var pool: BitmapPool
    private lateinit var fetcher: PdfPageUriFetcher

    @Before
    fun before() {
        context = ApplicationProvider.getApplicationContext()
        pool = BitmapPool(0)
        fetcher = PdfPageUriFetcher(context)
    }

    // TODO
}
