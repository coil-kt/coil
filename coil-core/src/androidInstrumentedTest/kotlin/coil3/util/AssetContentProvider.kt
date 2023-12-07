package coil3.util

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import okio.buffer
import okio.sink
import okio.source

/**
 * A [ContentProvider] that returns images from the Android assets directory.
 *
 * Valid format: content://coil/normal.jpg
 */
class AssetContentProvider : ContentProvider() {

    override fun onCreate() = true

    override fun insert(
        uri: Uri,
        values: ContentValues?
    ): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        // Copy the file out of the private assets directory.
        val context = checkNotNull(context)
        val source = context.assets.open(uri.pathSegments.joinToString("/")).source()
        val parent = File(context.cacheDir.path + File.separator + uri.pathSegments.dropLast(1)).apply { mkdirs() }
        val file = File(parent, uri.pathSegments.last())
        val sink = file.sink().buffer()
        source.use { sink.use { sink.writeAll(source) } }

        // Return a cursor containing the image's file path.
        val cursor = MatrixCursor(arrayOf("_data"))
        cursor.addRow(arrayOf(file.path))
        return cursor
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ) = 0

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ) = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor = openFileHelper(uri, mode)

    override fun getType(uri: Uri) = "image/jpeg"
}
