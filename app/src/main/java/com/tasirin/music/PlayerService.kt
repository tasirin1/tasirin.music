package com.tasirin.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class PlayerService : Service() {

    companion object {
        const val ACTION_PLAY_INDEX = "com.tasirin.music.PLAY_INDEX"
        const val ACTION_TOGGLE = "com.tasirin.music.TOGGLE"
        const val ACTION_NEXT = "com.tasirin.music.NEXT"
        const val ACTION_PREV = "com.tasirin.music.PREV"
        const val ACTION_SEEK = "com.tasirin.music.SEEK"
        const val ACTION_STOP = "com.tasirin.music.STOP"
        const val ACTION_UPDATE = "com.tasirin.music.UPDATE"

        const val EXTRA_INDEX = "extra_index"
        const val EXTRA_POS = "extra_pos"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_PLAYING = "extra_playing"
        const val EXTRA_POSITION = "extra_position"
        const val EXTRA_DURATION = "extra_duration"

        private const val CHANNEL_ID = "playback"
        private const val NOTIF_ID = 1
    }

    private var player: MediaPlayer? = null
    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1
    private var prepared: Boolean = false
    private var foregroundStarted: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            broadcastUpdate()
            if (prepared) {
                handler.postDelayed(this, 1000)
            }
        }
    }

    private var focusRequest: AudioFocusRequest? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            if (player?.isPlaying == true) {
                player?.pause()
                afterStateChange()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnPreparedListener {
                prepared = true
                it.start()
                afterStateChange()
                handler.removeCallbacks(ticker)
                handler.post(ticker)
            }
            setOnCompletionListener { playNext(auto = true) }
            setOnErrorListener { _, _, _ ->
                prepared = false
                broadcastUpdate()
                true
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureForeground()
        when (intent?.action) {
            ACTION_PLAY_INDEX -> {
                ensurePlaylist()
                val index = intent.getIntExtra(EXTRA_INDEX, -1)
                if (index in playlist.indices) {
                    playIndex(index)
                }
            }
            ACTION_TOGGLE -> toggle()
            ACTION_NEXT -> playNext(auto = false)
            ACTION_PREV -> playPrev()
            ACTION_SEEK -> {
                if (prepared) {
                    val pos = intent.getIntExtra(EXTRA_POS, 0)
                    player?.seekTo(pos)
                    broadcastUpdate()
                }
            }
            ACTION_STOP -> {
                player?.stop()
                prepared = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        abandonFocus()
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun ensurePlaylist() {
        if (playlist.isEmpty()) {
            playlist = SongLoader.load(this)
        }
    }

    private fun playIndex(index: Int) {
        val p = player ?: return
        currentIndex = index
        prepared = false
        requestFocus()
        try {
            p.reset()
            p.setDataSource(this, playlist[index].uri)
            p.prepareAsync()
        } catch (e: Exception) {
            broadcastUpdate()
        }
        updateNotification()
        broadcastUpdate()
    }

    private fun toggle() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        } else if (prepared) {
            requestFocus()
            p.start()
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        }
        afterStateChange()
    }

    private fun playNext(auto: Boolean) {
        if (playlist.isEmpty() || currentIndex < 0) return
        val next = currentIndex + 1
        if (next in playlist.indices) {
            playIndex(next)
        } else {
            prepared = false
            if (!auto) currentIndex = -1
            updateNotification()
            broadcastUpdate()
        }
    }

    private fun playPrev() {
        if (playlist.isEmpty() || currentIndex < 0) return
        val p = player ?: return
        if (prepared && p.currentPosition > 3000) {
            p.seekTo(0)
            broadcastUpdate()
            return
        }
        val prev = currentIndex - 1
        if (prev in playlist.indices) {
            playIndex(prev)
        } else {
            p.seekTo(0)
            broadcastUpdate()
        }
    }

    private fun afterStateChange() {
        updateNotification()
        broadcastUpdate()
    }

    private fun currentSong(): Song? = playlist.getOrNull(currentIndex)

    private fun broadcastUpdate() {
        val song = currentSong()
        val intent = Intent(ACTION_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_INDEX, currentIndex)
            putExtra(EXTRA_TITLE, song?.title ?: "")
            putExtra(EXTRA_ARTIST, song?.artist ?: "")
            putExtra(EXTRA_PLAYING, player?.isPlaying == true)
            putExtra(EXTRA_POSITION, if (prepared) player?.currentPosition ?: 0 else 0)
            putExtra(EXTRA_DURATION, if (prepared) player?.duration ?: 0 else (song?.durationMs?.toInt() ?: 0))
        }
        sendBroadcast(intent)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun ensureForeground() {
        if (!foregroundStarted) {
            startForeground(NOTIF_ID, buildNotification())
            foregroundStarted = true
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        val notification = buildNotification()
        if (foregroundStarted) {
            manager.notify(NOTIF_ID, notification)
        } else {
            startForeground(NOTIF_ID, notification)
            foregroundStarted = true
        }
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, PlayerService::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getService(this, requestCode, intent, flags)
    }

    private fun buildNotification(): Notification {
        val song = currentSong()
        val playing = player?.isPlaying == true
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(song?.title ?: getString(R.string.app_name))
            .setContentText(song?.artist ?: getString(R.string.loading))
            .setContentIntent(contentIntent)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .addAction(
                android.R.drawable.ic_media_previous,
                "Prev",
                servicePendingIntent(ACTION_PREV, 1)
            )
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Pause" else "Play",
                servicePendingIntent(ACTION_TOGGLE, 2)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                servicePendingIntent(ACTION_NEXT, 3)
            )
            .build()
    }

    private fun requestFocus() {
        val audioManager = getSystemService(AudioManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusListener)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                focusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonFocus() {
        val audioManager = getSystemService(AudioManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
    }
}
