package code.name.monkey.retromusic.fragments.downloader

import android.content.Intent
import android.os.Bundle
import android.transition.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
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
import code.name.monkey.retromusic.fragments.downloader.DownloaderFragment.Companion.NOTIFICATION_CHANNEL_ID
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.util.DownloaderUtil
import code.name.monkey.retromusic.util.MediaStoreUtil
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
            download(it)
        }
    }

    private fun download(url: String) {
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
        val data = Data.Builder()
            .putString(DownloadWorker.CHANNEL_ID_KEY, NOTIFICATION_CHANNEL_ID)
            .putString("url", url)
            .build()
        val request = OneTimeWorkRequestBuilder<DownloadWorker>().setInputData(data).build()
        context?.let {
            val workManager = WorkManager.getInstance(it)
            workManager.enqueue(request)
            workManager.getWorkInfoByIdLiveData(request.id).observe(this) { info ->
                val uri = info.outputData.getString("uri")
                val author = info.outputData.getString("author")
                val title = info.outputData.getString("title")
                if (uri != null) {
                    val tagEditorIntent = DownloaderUtil.getTagEditorIntent(title, author, uri, it)
                    activity?.startActivity(tagEditorIntent)
                }
            }
        } ?: Log.e(TAG, "Context is null")
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

    private fun setUpViews() {
        applyToolbar(binding.toolbar)
        viewModel.searchTerm?.let {
            binding.searchView.setText(it)
        }
        checkForPadding()
        binding.searchResults.layoutManager = LinearLayoutManager(context)
        val results = viewModel.results.value
        results?.let {
            if (it.results != null && it.results.isNotEmpty()) {
                (binding.searchResults.adapter as YTSearchAdapter).update(it.results)
            }
        }
        binding.loadingIndicator.visibility = View.VISIBLE
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