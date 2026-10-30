package com.tasirin.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat

class PlayerService : Service() {

    companion object {
        const val ACTION_PLAY_INDEX = "com.tasirin.music.PLAY_INDEX"
        const val ACTION_PLAY_ID = "com.tasirin.music.PLAY_ID"
        const val ACTION_PLAY_LIST = "com.tasirin.music.PLAY_LIST"
        const val ACTION_TOGGLE = "com.tasirin.music.TOGGLE"
        const val ACTION_PLAY = "com.tasirin.music.PLAY"
        const val ACTION_PAUSE = "com.tasirin.music.PAUSE"
        const val ACTION_NEXT = "com.tasirin.music.NEXT"
        const val ACTION_PREV = "com.tasirin.music.PREV"
        const val ACTION_SEEK = "com.tasirin.music.SEEK"
        const val ACTION_STOP = "com.tasirin.music.STOP"
        const val ACTION_TOGGLE_SHUFFLE = "com.tasirin.music.TOGGLE_SHUFFLE"
        const val ACTION_CYCLE_REPEAT = "com.tasirin.music.CYCLE_REPEAT"
        const val ACTION_SLEEP_TIMER = "com.tasirin.music.SLEEP_TIMER"
        const val ACTION_UPDATE = "com.tasirin.music.UPDATE"

        const val EXTRA_INDEX = "extra_index"
        const val EXTRA_IDS = "extra_ids"
        const val EXTRA_SONG_ID = "extra_song_id"
        const val EXTRA_MINUTES = "extra_minutes"
        const val EXTRA_POS = "extra_pos"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_PLAYING = "extra_playing"
        const val EXTRA_POSITION = "extra_position"
        const val EXTRA_DURATION = "extra_duration"
        const val EXTRA_SHUFFLE = "extra_shuffle"
        const val EXTRA_REPEAT = "extra_repeat"
        const val EXTRA_SLEEP_LEFT_SEC = "extra_sleep_left_sec"

        const val REPEAT_OFF = 0
        const val REPEAT_ALL = 1
        const val REPEAT_ONE = 2

        private const val CHANNEL_ID = "playback"
        private const val NOTIF_ID = 1

        private const val PREFS_NAME = "player_state"
        private const val KEY_SONG_ID = "song_id"
        private const val KEY_POSITION = "position"
        private const val KEY_SHUFFLE = "shuffle"
        private const val KEY_REPEAT = "repeat"
    }

    private var player: MediaPlayer? = null

    /** Seluruh pustaka lagu di perangkat; playlist adalah konteks putar aktif. */
    private var library: List<Song> = emptyList()
    private var playlist: List<Song> = emptyList()
    private var currentIndex: Int = -1
    private var prepared: Boolean = false
    private var foregroundStarted: Boolean = false

    // Urutan putar aktif: daftar indeks ke playlist. Saat acak aktif, urutan
    // ini adalah hasil pengacakan dengan lagu berjalan di posisi orderPos.
    private var playOrder: MutableList<Int> = mutableListOf()
    private var orderPos: Int = -1
    private var shuffle: Boolean = false
    private var repeatMode: Int = REPEAT_OFF

    private var playWhenReady: Boolean = true
    private var pendingSeekMs: Int = 0
    private var restored: Boolean = false

    private lateinit var session: MediaSessionCompat
    private var artwork: Bitmap? = null
    private var artworkAlbumId: Long = -1L

    private var sleepDeadlineMs: Long = 0L
    private val sleepRunnable = Runnable {
        sleepDeadlineMs = 0L
        pause()
    }

    private val handler = Handler(Looper.getMainLooper())
    private var tickCount: Int = 0
    private val ticker = object : Runnable {
        override fun run() {
            broadcastUpdate()
            tickCount++
            if (tickCount % 15 == 0) {
                saveState()
            }
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

    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            play()
        }

        override fun onPause() {
            pause()
        }

        override fun onSkipToNext() {
            playNext(auto = false)
        }

        override fun onSkipToPrevious() {
            playPrev()
        }

        override fun onSeekTo(pos: Long) {
            seekTo(pos.toInt())
        }

        override fun onStop() {
            stopPlayback()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        session = MediaSessionCompat(this, "TasirinMusic").apply {
            setCallback(sessionCallback)
            isActive = true
        }
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnPreparedListener {
                prepared = true
                if (pendingSeekMs > 0) {
                    runCatching { it.seekTo(pendingSeekMs) }
                    pendingSeekMs = 0
                }
                if (playWhenReady) {
                    it.start()
                }
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
        ensurePlaylist()
        restoreIfNeeded()
        when (intent?.action) {
            ACTION_PLAY_INDEX -> {
                playlist = library
                val index = intent.getIntExtra(EXTRA_INDEX, -1)
                if (index in playlist.indices) {
                    playIndex(index, rebuildOrder = true)
                }
            }
            ACTION_PLAY_LIST -> {
                val ids = intent.getLongArrayExtra(EXTRA_IDS)
                val pos = intent.getIntExtra(EXTRA_POS, 0)
                playList(ids, pos)
            }
            ACTION_PLAY_ID -> {
                val songId = intent.getLongExtra(EXTRA_SONG_ID, -1L)
                val index = playlist.indexOfFirst { it.id == songId }
                if (index >= 0) {
                    playIndex(index, rebuildOrder = true)
                }
            }
            ACTION_TOGGLE -> toggle()
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> playNext(auto = false)
            ACTION_PREV -> playPrev()
            ACTION_SEEK -> seekTo(intent.getIntExtra(EXTRA_POS, 0))
            ACTION_TOGGLE_SHUFFLE -> setShuffle(!shuffle)
            ACTION_CYCLE_REPEAT -> {
                repeatMode = (repeatMode + 1) % 3
                afterStateChange()
            }
            ACTION_SLEEP_TIMER -> setSleepTimer(intent.getIntExtra(EXTRA_MINUTES, 0))
            ACTION_STOP -> stopPlayback()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        handler.removeCallbacks(sleepRunnable)
        saveState()
        if (::session.isInitialized) {
            session.release()
        }
        abandonFocus()
        player?.release()
        player = null
        super.onDestroy()
    }

    // ---------------------------------------------------------------- antrean

    private fun ensurePlaylist() {
        if (library.isEmpty()) {
            library = SongLoader.load(this)
        }
        if (playlist.isEmpty()) {
            playlist = library
            if (playOrder.isEmpty()) {
                playOrder = playlist.indices.toMutableList()
            }
        }
    }

    /**
     * Putar daftar lagu tertentu (mis. isi sebuah album/artis/folder) sebagai
     * konteks antrean baru, mulai dari posisi [pos] di daftar itu.
     */
    private fun playList(ids: LongArray?, pos: Int) {
        if (ids == null || ids.isEmpty() || library.isEmpty()) return
        val byId = library.associateBy { it.id }
        val newList = ids.mapNotNull { byId[it] }
        if (newList.isEmpty()) return
        playlist = newList
        val start = pos.coerceIn(0, newList.lastIndex)
        playIndex(start, rebuildOrder = true)
    }

    private fun rebuildOrder(startIndex: Int) {
        if (playlist.isEmpty()) {
            playOrder = mutableListOf()
            orderPos = -1
            return
        }
        playOrder = if (shuffle) {
            val rest = playlist.indices.filter { it != startIndex }.shuffled()
            (listOf(startIndex) + rest).toMutableList()
        } else {
            playlist.indices.toMutableList()
        }
        orderPos = playOrder.indexOf(startIndex)
    }

    private fun playIndex(index: Int, rebuildOrder: Boolean) {
        if (index !in playlist.indices) return
        currentIndex = index
        if (rebuildOrder) {
            rebuildOrder(index)
        }
        prepareCurrent(autoplay = true, seekMs = 0)
    }

    private fun prepareCurrent(autoplay: Boolean, seekMs: Int) {
        val p = player ?: return
        val song = currentSong() ?: return
        prepared = false
        playWhenReady = autoplay
        pendingSeekMs = seekMs
        if (autoplay) {
            requestFocus()
        }
        try {
            p.reset()
            p.setDataSource(this, song.uri)
            p.prepareAsync()
        } catch (e: Exception) {
            broadcastUpdate()
        }
        loadArtwork(song)
        afterStateChange()
    }

    private fun playNext(auto: Boolean) {
        if (playlist.isEmpty() || currentIndex < 0) return
        val p = player ?: return
        if (auto && repeatMode == REPEAT_ONE && prepared) {
            runCatching {
                p.seekTo(0)
                p.start()
            }
            afterStateChange()
            return
        }
        when {
            orderPos < playOrder.lastIndex -> {
                orderPos++
                playIndex(playOrder[orderPos], rebuildOrder = false)
            }
            repeatMode == REPEAT_ALL && playOrder.isNotEmpty() -> {
                orderPos = 0
                playIndex(playOrder[0], rebuildOrder = false)
            }
            else -> {
                prepared = false
                if (!auto) {
                    currentIndex = -1
                }
                afterStateChange()
            }
        }
    }

    private fun playPrev() {
        if (playlist.isEmpty() || currentIndex < 0) return
        val p = player ?: return
        if (prepared && runCatching { p.currentPosition }.getOrDefault(0) > 3000) {
            runCatching { p.seekTo(0) }
            broadcastUpdate()
            return
        }
        if (orderPos > 0) {
            orderPos--
            playIndex(playOrder[orderPos], rebuildOrder = false)
        } else {
            runCatching { p.seekTo(0) }
            broadcastUpdate()
        }
    }

    // -------------------------------------------------------------- transport

    private fun play() {
        val p = player ?: return
        if (!prepared && currentIndex >= 0) {
            // Lagu hasil pulih sesi belum selesai disiapkan: putar begitu siap.
            playWhenReady = true
        }
        if (prepared && !p.isPlaying) {
            requestFocus()
            p.start()
            handler.removeCallbacks(ticker)
            handler.post(ticker)
        }
        afterStateChange()
    }

    private fun pause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        }
        afterStateChange()
    }

    private fun toggle() {
        if (player?.isPlaying == true) {
            pause()
        } else {
            play()
        }
    }

    private fun seekTo(posMs: Int) {
        if (prepared) {
            runCatching { player?.seekTo(posMs) }
            broadcastUpdate()
        }
    }

    private fun stopPlayback() {
        setSleepTimer(0)
        runCatching { player?.stop() }
        prepared = false
        currentIndex = -1
        orderPos = -1
        syncSharedState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        stopSelf()
    }

    private fun setShuffle(on: Boolean) {
        shuffle = on
        if (currentIndex >= 0) {
            rebuildOrder(currentIndex)
        }
        afterStateChange()
    }

    // ----------------------------------------------------------- timer tidur

    private fun setSleepTimer(minutes: Int) {
        handler.removeCallbacks(sleepRunnable)
        if (minutes <= 0) {
            sleepDeadlineMs = 0L
        } else {
            sleepDeadlineMs = SystemClock.elapsedRealtime() + minutes * 60_000L
            handler.postDelayed(sleepRunnable, minutes * 60_000L)
        }
        broadcastUpdate()
    }

    private fun sleepLeftSec(): Int {
        if (sleepDeadlineMs <= 0L) return -1
        val left = (sleepDeadlineMs - SystemClock.elapsedRealtime()) / 1000L
        return left.toInt().coerceAtLeast(0)
    }

    // --------------------------------------------------------- simpan/pulih

    private fun restoreIfNeeded() {
        if (restored) return
        restored = true
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        shuffle = prefs.getBoolean(KEY_SHUFFLE, false)
        repeatMode = prefs.getInt(KEY_REPEAT, REPEAT_OFF)
        val songId = prefs.getLong(KEY_SONG_ID, -1L)
        val savedPos = prefs.getInt(KEY_POSITION, 0)
        if (songId <= 0L || playlist.isEmpty()) {
            syncSharedState()
            return
        }
        val index = playlist.indexOfFirst { it.id == songId }
        if (index < 0) {
            syncSharedState()
            return
        }
        currentIndex = index
        rebuildOrder(index)
        prepareCurrent(autoplay = false, seekMs = savedPos)
    }

    private fun saveState() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_SONG_ID, currentSong()?.id ?: -1L)
            .putInt(KEY_POSITION, currentPositionMs())
            .putBoolean(KEY_SHUFFLE, shuffle)
            .putInt(KEY_REPEAT, repeatMode)
            .apply()
    }

    private fun currentPositionMs(): Int = when {
        prepared -> runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)
        pendingSeekMs > 0 -> pendingSeekMs
        else -> 0
    }

    // ------------------------------------------------------------ sampul seni

    private fun loadArtwork(song: Song) {
        if (song.albumId == artworkAlbumId) return
        artworkAlbumId = song.albumId
        artwork = null
        val albumId = song.albumId
        Thread {
            val bitmap = ArtworkLoader.loadBitmap(this, albumId)
            if (bitmap != null && albumId == artworkAlbumId) {
                artwork = bitmap
                handler.post {
                    updateNotification()
                    syncSessionMetadata()
                }
            }
        }.start()
    }

    // ------------------------------------------------------- sesi media & UI

    private fun syncSharedState() {
        PlaybackState.playlist = playlist
        PlaybackState.order = playOrder.toList()
        PlaybackState.orderPos = orderPos
        PlaybackState.currentSongId = currentSong()?.id ?: -1L
    }

    private fun syncSessionMetadata() {
        if (!::session.isInitialized) return
        val song = currentSong() ?: return
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.durationMs)
        artwork?.let {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
        }
        session.setMetadata(builder.build())
    }

    private fun syncSessionState() {
        if (!::session.isInitialized) return
        val playing = runCatching { player?.isPlaying == true }.getOrDefault(false)
        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SEEK_TO or
            PlaybackStateCompat.ACTION_STOP
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(
                    if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                    currentPositionMs().toLong(),
                    1f
                )
                .build()
        )
    }

    private fun afterStateChange() {
        syncSharedState()
        updateNotification()
        broadcastUpdate()
        syncSessionMetadata()
        syncSessionState()
        saveState()
    }

    private fun currentSong(): Song? = playlist.getOrNull(currentIndex)

    private fun broadcastUpdate() {
        val song = currentSong()
        val intent = Intent(ACTION_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_INDEX, currentIndex)
            putExtra(EXTRA_SONG_ID, song?.id ?: -1L)
            putExtra(EXTRA_TITLE, song?.title ?: "")
            putExtra(EXTRA_ARTIST, song?.artist ?: "")
            putExtra(EXTRA_PLAYING, runCatching { player?.isPlaying == true }.getOrDefault(false))
            putExtra(EXTRA_POSITION, currentPositionMs())
            putExtra(
                EXTRA_DURATION,
                if (prepared) {
                    runCatching { player?.duration ?: 0 }.getOrDefault(0)
                } else {
                    song?.durationMs?.toInt() ?: 0
                }
            )
            putExtra(EXTRA_SHUFFLE, shuffle)
            putExtra(EXTRA_REPEAT, repeatMode)
            putExtra(EXTRA_SLEEP_LEFT_SEC, sleepLeftSec())
        }
        sendBroadcast(intent)
        syncSessionState()
    }

    // -------------------------------------------------------------- notifikasi

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
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentTitle(song?.title ?: getString(R.string.app_name))
            .setContentText(song?.artist ?: getString(R.string.loading))
            .setSubText(song?.album?.takeIf { it.isNotBlank() })
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
        artwork?.let { builder.setLargeIcon(it) }
        if (::session.isInitialized) {
            builder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }
        return builder.build()
    }

    // -------------------------------------------------------------- fokus audio

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
