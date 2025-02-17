package com.ichbinluka.downloader.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.tageditor.AbsTagEditorActivity
import code.name.monkey.retromusic.activities.tageditor.SongTagEditorActivity
import code.name.monkey.retromusic.util.DownloaderUtil
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.util.*

class DownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(
    params = params, appContext = context
) {
    private val ytDL: YoutubeDL by lazy {
        YoutubeDL.getInstance()
    }

    private val channelId: String = inputData.getString(CHANNEL_ID_KEY) ?: ""

    private val notification: NotificationCompat.Builder = NotificationCompat.Builder(
        context,
        channelId
    )
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentTitle(applicationContext.getString(R.string.notification_downloading))
        .setOnlyAlertOnce(true)
        .setProgress(100, 0, false)
        .setAutoCancel(true)
        .setSmallIcon(R.drawable.ic_download_music)

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        initNotificationChannel()
        notificationManager.notify(0, notification.build())


        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "music")
        val url = inputData.getString("url")
        if (url != "" && url != null) {
            val requestUrl = if (url.contains("https://")) { url } else { "ytsearch:\"$url\"" }
            val request = YoutubeDLRequest(requestUrl).apply {
                addOption("-o", "${dir.absolutePath}/%(title)s.%(ext)s")
                addOption("-S", "ext")
                addOption("--no-mtime") // Use current time as last modified date
                addOption("-f", "bestaudio")
                addOption("--extract-audio")
                addOption("--audio-format", "mp3")
                addOption("--audio-quality", 0)
            }
            withContext(Dispatchers.IO) {
                try {
                    val info = ytDL.getInfo(requestUrl)
                    notification.setContentTitle(applicationContext.getString(R.string.notification_downloading, info.title))
                    updateNotification()
                    val response = ytDL.execute(request) {
                            progress: Float, _, _ ->
                        notification.setProgress(100, progress.toInt(), false)
                        updateNotification()
                    }
                    //Log.d(TAG, response.out)
                    val mergerIndex = response.out.lastIndexOf(MERGER_TERM)
                    val index = if (mergerIndex == -1) {
                        response.out.lastIndexOf(DESTINATION_TERM) + DESTINATION_TERM.length + 1
                    } else {
                        mergerIndex + MERGER_TERM.length + 1
                    }

                    val path = response.out.substring(startIndex = index).takeWhile { it != '\n' && it != '"' }
                    // Manually set the last modified date to current time since yt-dlp does not seem
                    // to do this very reliable
                    val file = File(path)
                    file.setLastModified(Calendar.getInstance().timeInMillis)

                    MediaScannerConnection.scanFile(applicationContext, arrayOf(path), null) { s, uri ->
                        val intent = DownloaderUtil.getTagEditorIntent(info.title, info.uploader, path, applicationContext)
                        val flags = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        } else {
                            PendingIntent.FLAG_UPDATE_CURRENT
                        }
                        Log.d(TAG, intent.getStringExtra(AbsTagEditorActivity.TITLE_ID).toString())
                        notification.apply {
                            setProgress(0, 0, false)
                            setContentIntent(PendingIntent.getActivity(applicationContext, 0, intent, flags, null))
                            setContentTitle(applicationContext.getString(R.string.notification_finished))
                            setContentText(applicationContext.getString(R.string.tap_to_open_in_tag_editor))
                            setOnlyAlertOnce(false)
                        }
                        updateNotification()
                    }
                    val data = Data.Builder()
                        .putString("uri", path)
                        .putString("title", info.title)
                        .putString("author", info.uploader)
                        .build()
                    Log.d(TAG, data.toString())
                    return@withContext Result.success(data)

                } catch (e: Exception) {
                    Log.e(TAG, e.message ?: "")
                    notification.setContentTitle(applicationContext.getString(R.string.error_while_downloading))
                    notification.setProgress(0, 0, false)
                    updateNotification()
                    return@withContext Result.failure()
                }
            }

        }
        return Result.success()
    }

    private fun updateNotification() {
        notificationManager.notify(0, notification.build())
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Downloader",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Downloading"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID_KEY = "channel_id"
        const val TAG = "Download Worker"
        private const val DESTINATION_TERM = "Destination:"
        private const val MERGER_TERM = "[Merger] Merging formats into "
    }
}