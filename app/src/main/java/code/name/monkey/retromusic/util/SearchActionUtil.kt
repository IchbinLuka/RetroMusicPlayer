package code.name.monkey.retromusic.util

import android.content.Context
import android.util.Log
import android.view.inputmethod.EditorInfo
import code.name.monkey.retromusic.fragments.downloader.DownloaderMainFragment
import code.name.monkey.retromusic.fragments.downloader.DownloaderViewModel
import com.google.android.material.textfield.TextInputEditText

object SearchActionUtil {
    inline fun configureSearchBar(
        searchView: TextInputEditText,
        viewModel: DownloaderViewModel,
        context: Context?,
        crossinline onSearch: (String) -> Unit = {},
        crossinline onDownload: (String) -> Unit = {}
    ) {
        searchView.setOnEditorActionListener { _, id, _ ->
            var out = false
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                searchView.onEditorAction(EditorInfo.IME_ACTION_DONE)
                val text = searchView.text.toString()
                if (text.contains("https://") || !viewModel.googleApiKeyConfigured) {
                    context?.let {
                        viewModel.download(text, it)
                    } ?: Log.e(DownloaderMainFragment.TAG, "context must not be null")
                    onDownload(text)
                } else {
                    viewModel.searchVideos(text)
                    onSearch(text)
                }
                searchView.text?.clear()
                out = true
            }
            out
        }
    }
}