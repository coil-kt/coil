package coil.util

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import coil.Coil

/**
 * A [ContentProvider] whose [Context] is used to initialize the [Coil] singleton.
 */
internal class CoilContentProvider : ContentProvider() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        internal lateinit var context: Context
            private set
    }

    @Suppress("RedundantCompanionReference")
    override fun onCreate(): Boolean {
        Companion.context = checkNotNull(context)
        return true
    }

    override fun insert(
        uri: Uri,
        values: ContentValues?
    ) = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ) = null

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

    override fun getType(uri: Uri) = null
}
