package code.name.monkey.retromusic.fragments.downloader

import android.content.Intent
import android.os.Bundle
import android.transition.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.tageditor.AbsTagEditorActivity
import code.name.monkey.retromusic.activities.tageditor.SongTagEditorActivity
import code.name.monkey.retromusic.adapter.YTSearchAdapter
import code.name.monkey.retromusic.databinding.FragmentDownloadMainBinding
import code.name.monkey.retromusic.databinding.FragmentDownloadSearchResultsBinding
import code.name.monkey.retromusic.extensions.applyToolbar
import code.name.monkey.retromusic.extensions.dip
import code.name.monkey.retromusic.extensions.elevatedAccentColor
import code.name.monkey.retromusic.fragments.downloader.DownloaderFragment.Companion.NOTIFICATION_CHANNEL_ID
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.util.DownloaderUtil
import code.name.monkey.retromusic.util.MediaStoreUtil
import code.name.monkey.retromusic.util.SearchActionUtil
import com.google.api.services.youtube.model.SearchResult
import com.ichbinluka.downloader.workers.DownloadWorker
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class DownloaderSearchFragment : Fragment() {
    private var _binding: FragmentDownloadSearchResultsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DownloaderViewModel by activityViewModels()

    private var resultData: List<SearchResult> = listOf()
    private val adapter = YTSearchAdapter(resultData) {
        if (!viewModel.downloading) {
            context?.let { it1 -> viewModel.download(it, it1) } ?: Log.e(TAG, "Context must not be null")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadSearchResultsBinding.inflate(inflater)
        setUpListeners()
        setUpViews()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkForPadding()
    }

    private fun setUpViews() {
        applyToolbar(binding.toolbar)
        viewModel.searchTerm?.let {
            binding.searchView.setText(it)
        }
        binding.searchResults.layoutManager = LinearLayoutManager(context)
        val results = viewModel.results.value
        results?.let {
            if (it.results != null && it.results.isNotEmpty()) {
                (binding.searchResults.adapter as YTSearchAdapter).update(it.results)
            }
        }
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.loadingIndicator.elevatedAccentColor()
    }

    private fun setUpListeners() {
        binding.searchResults.adapter = adapter
        viewModel.results.observeForever {
            binding.loadingIndicator.visibility = View.GONE
            if (it.successful) {
                Log.d("Downloader", it.results.toString())
                val adapter: YTSearchAdapter = binding.searchResults.adapter as YTSearchAdapter
                if (it.results != null) {
                    adapter.update(it.results)
                }
            } else {
                Log.e(TAG, "Searching failed")
            }
        }
        binding.clearText.setOnClickListener {
            binding.searchView.text?.clear()
        }
        binding.searchView.addTextChangedListener {
            if (it != null && it.isNotEmpty()) {
                binding.clearText.visibility = View.VISIBLE
            } else if (binding.clearText.visibility == View.VISIBLE) {
                binding.clearText.visibility = View.INVISIBLE
            }
        }
        /*binding.searchView.setOnEditorActionListener { _, id, _ ->
            var out = false
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                binding.searchView.onEditorAction(EditorInfo.IME_ACTION_DONE)
                val text = binding.searchView.text.toString()
                if (text.contains("https://") || !viewModel.googleApiKeyConfigured) {
                    context?.let {
                        viewModel.download(text, it)
                    } ?: Log.e(DownloaderMainFragment.TAG, "context must not be null")
                } else {
                    viewModel.searchVideos(text)
                }
                binding.searchView.text?.clear()
                out = true
            }
            out
        }*/
        SearchActionUtil.configureSearchBar(binding.searchView, viewModel, context)
    }

    private fun checkForPadding() {
        val itemCount: Int = adapter.itemCount

        if (itemCount > 0 && MusicPlayerRemote.playingQueue.isNotEmpty()) {
            binding.searchResults.updatePadding(bottom = dip(R.dimen.mini_player_height_expanded))
        } else {
            binding.searchResults.updatePadding(bottom = dip(R.dimen.bottom_nav_height))
        }
    }

    companion object {
        const val TAG = "Downloader Searching"
    }
}