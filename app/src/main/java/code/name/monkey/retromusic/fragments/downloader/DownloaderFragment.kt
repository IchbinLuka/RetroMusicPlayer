package code.name.monkey.retromusic.fragments.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.tageditor.AbsTagEditorActivity
import code.name.monkey.retromusic.activities.tageditor.SongTagEditorActivity
import code.name.monkey.retromusic.adapter.YTSearchAdapter
import code.name.monkey.retromusic.databinding.FragmentDownloaderBinding
import code.name.monkey.retromusic.extensions.applyToolbar
import code.name.monkey.retromusic.extensions.drawNextToNavbar
import code.name.monkey.retromusic.extensions.elevatedAccentColor
import code.name.monkey.retromusic.fragments.search.clearText
import code.name.monkey.retromusic.util.MediaStoreUtil
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.api.services.youtube.model.SearchResult
import com.google.api.services.youtube.model.VideoListResponse
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import java.io.File
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.widget.addTextChangedListener
import code.name.monkey.retromusic.activities.base.AbsMusicServiceActivity
import code.name.monkey.retromusic.extensions.dip
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.network.LastFMService
import code.name.monkey.retromusic.util.PreferenceUtil
import org.koin.android.ext.android.inject


class DownloaderFragment : Fragment() {
    private var _binding: FragmentDownloaderBinding? = null
    private val binding get() = _binding!!

    private val googleApiKeyConfigured: Boolean = PreferenceUtil.googleDataApiKey ?: "" != ""

    var resultData: List<SearchResult> = listOf()
    private val adapter = YTSearchAdapter(resultData) {
        if (!model.downloading) {
            download(it)
        }
    }

    private val model: DownloaderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            YoutubeDL.getInstance().init(requireActivity().applicationContext)
            FFmpeg.getInstance().init(requireActivity().applicationContext)
        } catch (e: YoutubeDLException) {
            Log.e("Youtube", "Error")
        }
    }

    fun search() {
        val text = binding.searchView.text.toString()
        if (googleApiKeyConfigured && !text.contains("https://")) {
            binding.loadingIndicator.visibility = View.VISIBLE
            model.searchVideos(text)
        } else {
            download(text)
        }
    }

    fun download(link: String): Unit {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Retro Music",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Downloading"
            }
        }
        val builder = NotificationCompat.Builder(requireActivity().applicationContext, NOTIFICATION_CHANNEL_ID)
        builder
            .setContentTitle(getString(R.string.notification_downloading))
            .setSmallIcon(R.drawable.ic_download_music)

        Log.d("Downloader", MediaStore.Audio.Media.getContentUri("external").path!!)

        model.downloadVideo(link = link, builder = builder, context = requireContext(), id = id)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloaderBinding.inflate(layoutInflater)
        binding.progressBar.elevatedAccentColor()
        binding.loadingIndicator.elevatedAccentColor()
        val data = model.results.value!!.results
        if (data != null) {
            resultData = data
        }
        binding.searchResults.adapter = adapter
        checkForPadding()
        binding.searchResults.layoutManager = LinearLayoutManager(context)
        binding.toolbarContainer.drawNextToNavbar()
        binding.appBarLayout.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(requireContext())
        binding.clearText.setOnClickListener {
            binding.searchView.clearText()
        }
        binding.googleApiWarning.visibility = if (googleApiKeyConfigured) View.GONE else View.VISIBLE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyToolbar(binding.toolbar)

        model.progress.observeForever {
            binding.progressBar.progress = it
        }
        binding.searchView.setOnEditorActionListener { _, id, _ ->
            var out = false
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                binding.searchView.onEditorAction(EditorInfo.IME_ACTION_DONE)
                search()
                out = true
            }
            out
        }
        binding.searchView.addTextChangedListener {
            if (it != null && it.isNotEmpty()) {
                binding.clearText.visibility = View.VISIBLE
            } else if (binding.clearText.visibility == View.VISIBLE) {
                binding.clearText.visibility = View.INVISIBLE
            }
        }
        val value = model.results.value
        model.results.observeForever {
            if (it.successful) {
                Log.d("Downloader", it.results.toString())
                binding.loadingIndicator.visibility = View.GONE
                val adapter: YTSearchAdapter = binding.searchResults.adapter as YTSearchAdapter
                if (it.results != null) {
                    adapter.update(it.results)
                }
            }
        }
        model.downloadResult.observeForever {
            if (it.successful) {
                val song = it.songInfo
                val tagEditorIntent = Intent(activity, SongTagEditorActivity::class.java)
                tagEditorIntent.putExtra(AbsTagEditorActivity.TITLE_ID, song.title)
                tagEditorIntent.putExtra(AbsTagEditorActivity.AUTHOR_ID, song.author)
                tagEditorIntent.putExtra(AbsTagEditorActivity.EXTRA_ID, getSongId(song.path))
                activity?.startActivity(tagEditorIntent)
                MediaStoreUtil.addToMediaStore(File(song.path), requireContext())
            } else {
                val toast = Toast.makeText(context, R.string.error_while_downloading, Toast.LENGTH_SHORT)
                toast.show()
            }
            //binding.downloadButton.isEnabled = true
        }
    }



    private fun getSongId(path: String): Long {
        val cr = requireContext().contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection: String = MediaStore.Audio.Media.DATA
        val selArgs = arrayOf(path)
        val projection = arrayOf(MediaStore.Audio.Media._ID)

        val values = ContentValues()
        values.put(MediaStore.Audio.Media.DATA, path)

        val cursor = cr.query(uri, projection, "$selection=?", selArgs, null)
        var songId: Long = 0
        if (cursor != null && cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            songId = cursor.getString(idIndex).toLong()
        }
        return songId
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID: String = "yt_downloader"
    }

    private fun checkForPadding() {
        val itemCount: Int = adapter.itemCount

        if (itemCount > 0 && MusicPlayerRemote.playingQueue.isNotEmpty()) {
            binding.searchResults.updatePadding(bottom = dip(R.dimen.mini_player_height_expanded))
        } else {
            binding.searchResults.updatePadding(bottom = dip(R.dimen.bottom_nav_height))
        }
    }

}