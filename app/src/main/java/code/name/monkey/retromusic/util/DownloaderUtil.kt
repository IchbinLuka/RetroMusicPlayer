package code.name.monkey.retromusic.util

import android.content.Context
import android.content.Intent
import code.name.monkey.retromusic.activities.tageditor.AbsTagEditorActivity
import code.name.monkey.retromusic.activities.tageditor.SongTagEditorActivity

object DownloaderUtil {
    fun getTagEditorIntent(title: String?, author: String?, path: String, context: Context) : Intent {
        val tagEditorIntent = Intent(context, SongTagEditorActivity::class.java)
        tagEditorIntent.putExtra(AbsTagEditorActivity.TITLE_ID, title ?: "")
        tagEditorIntent.putExtra(AbsTagEditorActivity.AUTHOR_ID, author ?: "")
        tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_ID, MediaStoreUtil.getSongId(path, context))
        return tagEditorIntent
    }
}