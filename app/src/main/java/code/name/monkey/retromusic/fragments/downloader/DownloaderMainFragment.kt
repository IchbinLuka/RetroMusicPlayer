package code.name.monkey.retromusic.fragments.downloader

import android.content.res.ColorStateList
import android.os.Bundle
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
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentDownloadMainBinding
import code.name.monkey.retromusic.databinding.FragmentDownloaderBinding
import code.name.monkey.retromusic.extensions.*

class DownloaderMainFragment : Fragment() {
    private var _binding: FragmentDownloadMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DownloaderViewModel by activityViewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDownloadMainBinding.inflate(inflater)
        setUpListeners()
        setUpViews()
        return binding.root
    }

    private fun setUpViews() {
        applyToolbar(binding.toolbar)
        binding.searchBar.appHandleColor()
        val color = ThemeStore.textColorPrimary(requireContext())
        binding.titleText.apply {
            setTextColor(color)
        }
        context?.let {
            val backgroundColor = ColorUtil.withAlpha(it.accentColor(), 0.12f)
            binding.searchContainer.backgroundTintList = ColorStateList.valueOf(backgroundColor)
        }
    }

    private fun setUpListeners() {
        binding.clearText.setOnClickListener {
            binding.searchBar.text?.clear()
        }
        binding.searchBar.addTextChangedListener {
            if (it != null && it.isNotEmpty()) {
                binding.clearText.visibility = View.VISIBLE
            } else if (binding.clearText.visibility == View.VISIBLE) {
                binding.clearText.visibility = View.INVISIBLE
            }
        }
        binding.searchBar.setOnEditorActionListener { _, id, _ ->
            var out = false
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                binding.searchBar.onEditorAction(EditorInfo.IME_ACTION_DONE)
                viewModel.searchVideos(binding.searchBar.text.toString())
                findNavController().navigate(R.id.download_search_fragment)
                out = true
            }
            out
        }
    }
}