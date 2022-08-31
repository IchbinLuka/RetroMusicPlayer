/*
 * Copyright (c) 2020 Hemanth Savarla.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 */
package code.name.monkey.retromusic.activities.tageditor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.drawToBitmap
import androidx.core.widget.doAfterTextChanged
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.ActivitySongTagEditorBinding
import code.name.monkey.retromusic.extensions.*
import code.name.monkey.retromusic.glide.GlideApp
import code.name.monkey.retromusic.glide.palette.BitmapPaletteWrapper
import code.name.monkey.retromusic.model.ArtworkInfo
import code.name.monkey.retromusic.network.LastFMService
import code.name.monkey.retromusic.repository.SongRepository
import code.name.monkey.retromusic.util.ImageUtil
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.RetroColorUtil
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.shape.MaterialShapeDrawable
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.tag.FieldKey
import org.koin.android.ext.android.inject
import java.lang.Exception
import java.util.*

class SongTagEditorActivity : AbsTagEditorActivity<ActivitySongTagEditorBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivitySongTagEditorBinding =
        ActivitySongTagEditorBinding::inflate


    private val songRepository by inject<SongRepository>()
    private val service: LastFMService by inject()

    private var albumArtBitmap: Bitmap? = null
    private var deleteAlbumArt: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpViews()
        binding.autoFillButton?.setOnClickListener {
            runBlocking {
                try {
                    val song = service.trackInfo(
                        binding.songText.text.toString(),
                        binding.artistText.text.toString()
                    ).track
                    binding.albumText.setText(song.album?.title ?: "")
                    binding.yearText.setText(song.wiki?.published ?: "")
                    if (song.album != null) {
                        binding.albumArtistText.setText(song.album?.artist ?: "")
                        Picasso.get().load(song.album.image[2].text).into(object: Target {
                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                albumArtBitmap = bitmap
                                binding.editorImage.setImageBitmap(bitmap)
                                dataChanged()
                            }

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                Log.e(TAG, "Failed to load bitmap")
                            }

                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                                Log.d(TAG, "Loading image")
                            }

                        })
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.stackTraceToString())
                    val toast = Toast.makeText(this@SongTagEditorActivity, "Error", Toast.LENGTH_SHORT)
                    toast.show()
                }
            }
        }
        setSupportActionBar(binding.toolbar)
        binding.appBarLayout?.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpViews() {

        val title = intent.extras?.getString(TITLE_ID)
        val author = intent.extras?.getString(AUTHOR_ID)

        fillViewsWithFileTags()

        if (title != null && author != null) {
            binding.songText.setText(title)
            binding.albumArtistText.setText(author)
            binding.artistText.setText(author)
            dataChanged()
        }

        binding.songTextContainer.setTint(false)
        binding.composerContainer.setTint(false)
        binding.albumTextContainer.setTint(false)
        binding.artistContainer.setTint(false)
        binding.albumArtistContainer.setTint(false)
        binding.yearContainer.setTint(false)
        binding.genreContainer.setTint(false)
        binding.trackNumberContainer.setTint(false)
        binding.discNumberContainer.setTint(false)
        binding.lyricsContainer.setTint(false)

        binding.autoFillButton?.elevatedAccentColor()
        binding.songText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.albumText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.albumArtistText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.artistText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.genreText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.yearText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.trackNumberText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.discNumberText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.lyricsText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.songComposerText.appHandleColor().doAfterTextChanged { dataChanged() }
    }

    private fun fillViewsWithFileTags() {
        binding.songText.setText(songTitle)
        binding.albumArtistText.setText(albumArtist)
        binding.albumText.setText(albumTitle)
        binding.artistText.setText(artistName)
        binding.genreText.setText(genreName)
        binding.yearText.setText(songYear)
        binding.trackNumberText.setText(trackNumber)
        binding.discNumberText.setText(discNumber)
        binding.lyricsText.setText(lyrics)
        binding.songComposerText.setText(composer)
        println(songTitle + songYear)
    }

    override fun loadCurrentImage() {
        val bitmap = albumArt
        setImageBitmap(
            bitmap,
            RetroColorUtil.getColor(
                RetroColorUtil.generatePalette(bitmap),
                defaultFooterColor()
            )
        )
        deleteAlbumArt = false
    }

    override fun searchImageOnWeb() {
        searchWebFor(binding.songText.text.toString(), binding.artistText.text.toString())
    }

    override fun deleteImage() {
        setImageBitmap(
            BitmapFactory.decodeResource(resources, R.drawable.default_audio_art),
            defaultFooterColor()
        )
        deleteAlbumArt = true
        dataChanged()
    }

    override fun setColors(color: Int) {
        super.setColors(color)
        saveFab.backgroundTintList = ColorStateList.valueOf(color)
        ColorStateList.valueOf(
            MaterialValueHelper.getPrimaryTextColor(
                this,
                color.isColorLight
            )
        ).also {
            saveFab.iconTint = it
            saveFab.setTextColor(it)
        }
    }

    override fun save() {
        val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
        fieldKeyValueMap[FieldKey.TITLE] = binding.songText.text.toString()
        fieldKeyValueMap[FieldKey.ALBUM] = binding.albumText.text.toString()
        fieldKeyValueMap[FieldKey.ARTIST] = binding.artistText.text.toString()
        fieldKeyValueMap[FieldKey.GENRE] = binding.genreText.text.toString()
        fieldKeyValueMap[FieldKey.YEAR] = binding.yearText.text.toString()
        fieldKeyValueMap[FieldKey.TRACK] = binding.trackNumberText.text.toString()
        fieldKeyValueMap[FieldKey.DISC_NO] = binding.discNumberText.text.toString()
        fieldKeyValueMap[FieldKey.LYRICS] = binding.lyricsText.text.toString()
        fieldKeyValueMap[FieldKey.ALBUM_ARTIST] = binding.albumArtistText.text.toString()
        fieldKeyValueMap[FieldKey.COMPOSER] = binding.songComposerText.text.toString()
        writeValuesToFiles(
            fieldKeyValueMap, when {
                deleteAlbumArt -> ArtworkInfo(id, null)
                albumArtBitmap == null -> null
                else -> ArtworkInfo(id, albumArtBitmap!!)
            }
        )
    }

    override fun getSongPaths(): List<String> = listOf(songRepository.song(id).data)

    override fun getSongUris(): List<Uri> {
        val path = intent.extras?.getString(PATH_ID)
        return if (path != null) {
            listOf(Uri.parse(path))
        } else {
            listOf(MusicUtil.getSongFileUri(id))
        }
    }

    override fun loadImageFromFile(selectedFile: Uri?) {
        GlideApp.with(this@SongTagEditorActivity).asBitmapPalette().load(selectedFile)
            .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
            .into(object : ImageViewTarget<BitmapPaletteWrapper>(binding.editorImage) {
                override fun onResourceReady(
                    resource: BitmapPaletteWrapper,
                    transition: Transition<in BitmapPaletteWrapper>?
                ) {
                    RetroColorUtil.getColor(resource.palette, Color.TRANSPARENT)
                    albumArtBitmap = resource.bitmap?.let { ImageUtil.resizeBitmap(it, 2048) }
                    setImageBitmap(
                        albumArtBitmap,
                        RetroColorUtil.getColor(
                            resource.palette,
                            defaultFooterColor()
                        )
                    )
                    deleteAlbumArt = false
                    dataChanged()
                    setResult(Activity.RESULT_OK)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    showToast(R.string.error_load_failed, Toast.LENGTH_LONG)
                }

                override fun setResource(resource: BitmapPaletteWrapper?) {}
            })
    }

    companion object {
        val TAG: String = SongTagEditorActivity::class.java.simpleName
    }

    override val editorImage: ImageView
        get() = binding.editorImage
}
