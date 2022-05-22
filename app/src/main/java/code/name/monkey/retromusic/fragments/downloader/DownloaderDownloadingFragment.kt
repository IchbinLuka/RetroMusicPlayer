package code.name.monkey.retromusic.fragments.downloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import code.name.monkey.retromusic.databinding.FragmentDownloadDownloadingBinding
import code.name.monkey.retromusic.databinding.FragmentDownloadMainBinding

class DownloaderDownloadingFragment : Fragment() {
    private var _binding: FragmentDownloadDownloadingBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DownloaderViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadDownloadingBinding.inflate(inflater)

        return binding.root
    }
}