package code.name.monkey.retromusic.fragments.downloader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import code.name.monkey.retromusic.databinding.FragmentDownloadMainBinding
import code.name.monkey.retromusic.databinding.FragmentDownloadSearchResultsBinding

class DownloaderSearchFragment : Fragment() {
    private var _binding: FragmentDownloadSearchResultsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadSearchResultsBinding.inflate(inflater)
        return binding.root
    }
}