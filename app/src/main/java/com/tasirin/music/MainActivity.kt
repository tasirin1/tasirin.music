package com.tasirin.music

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.tasirin.music.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private enum class Mode { SONGS, ALBUMS, ARTISTS, FOLDERS, GENRES, CATEGORY }

    private enum class SortMode { TITLE, ARTIST, ALBUM, NEWEST, LONGEST }

    private lateinit var binding: ActivityMainBinding
    private lateinit var songAdapter: SongAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var albumAdapter: AlbumAdapter

    private var allSongs: List<Song> = emptyList()
    private var mode: Mode = Mode.SONGS
    private var lastTab: Mode = Mode.SONGS
    private var categorySource: Mode = Mode.ALBUMS
    private var sortMode: SortMode = SortMode.TITLE

    private var seekingMini: Boolean = false
    private var seekingNp: Boolean = false
    private var npSongId: Long = -1L
    private var lyricsVisible: Boolean = false
    private var currentLyrics: String? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = results[Manifest.permission.READ_MEDIA_AUDIO] == true ||
            results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (audioGranted) {
            loadSongs()
        } else {
            binding.emptyText.setText(R.string.empty_no_permission)
            binding.emptyText.visibility = View.VISIBLE
        }
    }

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val songId = intent.getLongExtra(PlayerService.EXTRA_SONG_ID, -1L)
            if (songId < 0L) {
                binding.miniPlayer.visibility = View.GONE
                return
            }
            binding.miniPlayer.visibility = View.VISIBLE
            val title = intent.getStringExtra(PlayerService.EXTRA_TITLE) ?: ""
            val artist = intent.getStringExtra(PlayerService.EXTRA_ARTIST) ?: ""
            binding.nowTitle.text = title
            binding.nowArtist.text = artist
            binding.npTitle.text = title
            binding.npArtist.text = artist
            val playing = intent.getBooleanExtra(PlayerService.EXTRA_PLAYING, false)
            val playIcon =
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            binding.btnPlayPause.setImageResource(playIcon)
            binding.npPlayPause.setImageResource(playIcon)
            val duration = intent.getIntExtra(PlayerService.EXTRA_DURATION, 0)
            val position = intent.getIntExtra(PlayerService.EXTRA_POSITION, 0)
            if (duration > 0) {
                binding.seekBar.max = duration
                binding.durText.text = formatDuration(duration.toLong())
                binding.npSeek.max = duration
                binding.npDur.text = formatDuration(duration.toLong())
            }
            if (!seekingMini) {
                binding.seekBar.progress = position
                binding.posText.text = formatDuration(position.toLong())
            }
            if (!seekingNp) {
                binding.npSeek.progress = position
                binding.npPos.text = formatDuration(position.toLong())
            }
            updateModeButtons(
                intent.getBooleanExtra(PlayerService.EXTRA_SHUFFLE, false),
                intent.getIntExtra(PlayerService.EXTRA_REPEAT, PlayerService.REPEAT_OFF),
                intent.getIntExtra(PlayerService.EXTRA_SLEEP_LEFT_SEC, -1)
            )
            songAdapter.setCurrentId(songId)
            if (songId != npSongId) {
                npSongId = songId
                onSongChangedForNp(songId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        songAdapter = SongAdapter { shownPosition ->
            val ids = songAdapter.shownIds()
            if (ids.isEmpty()) return@SongAdapter
            sendCommand(PlayerService.ACTION_PLAY_LIST) {
                putExtra(PlayerService.EXTRA_IDS, ids)
                putExtra(PlayerService.EXTRA_POS, shownPosition)
            }
        }
        categoryAdapter = CategoryAdapter { item -> openCategory(item) }
        albumAdapter = AlbumAdapter { item -> openCategory(item) }
        binding.songList.layoutManager = LinearLayoutManager(this)
        binding.songList.adapter = songAdapter

        binding.searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnPlayPause.setOnClickListener { sendCommand(PlayerService.ACTION_TOGGLE) {} }
        binding.btnNext.setOnClickListener { sendCommand(PlayerService.ACTION_NEXT) {} }
        binding.btnPrev.setOnClickListener { sendCommand(PlayerService.ACTION_PREV) {} }
        binding.npPlayPause.setOnClickListener { sendCommand(PlayerService.ACTION_TOGGLE) {} }
        binding.npNext.setOnClickListener { sendCommand(PlayerService.ACTION_NEXT) {} }
        binding.npPrev.setOnClickListener { sendCommand(PlayerService.ACTION_PREV) {} }
        binding.btnShuffle.setOnClickListener { sendCommand(PlayerService.ACTION_TOGGLE_SHUFFLE) {} }
        binding.npShuffle.setOnClickListener { sendCommand(PlayerService.ACTION_TOGGLE_SHUFFLE) {} }
        binding.btnRepeat.setOnClickListener { sendCommand(PlayerService.ACTION_CYCLE_REPEAT) {} }
        binding.npRepeat.setOnClickListener { sendCommand(PlayerService.ACTION_CYCLE_REPEAT) {} }
        binding.btnQueue.setOnClickListener { showQueueDialog() }
        binding.npQueue.setOnClickListener { showQueueDialog() }
        binding.btnSleep.setOnClickListener { showSleepDialog() }
        binding.npSleep.setOnClickListener { showSleepDialog() }
        binding.btnSort.setOnClickListener { showSortDialog() }

        binding.nowTitle.setOnClickListener { openNowPlaying() }
        binding.nowArtist.setOnClickListener { openNowPlaying() }
        binding.btnCloseNp.setOnClickListener { binding.nowPlaying.visibility = View.GONE }
        binding.btnLyrics.setOnClickListener {
            lyricsVisible = !lyricsVisible
            binding.npLyrics.visibility = if (lyricsVisible) View.VISIBLE else View.GONE
            if (lyricsVisible) {
                binding.npLyrics.text = currentLyrics ?: getString(R.string.lyrics_none)
            }
        }
        binding.npLyrics.movementMethod = ScrollingMovementMethod()

        binding.navSongs.setOnClickListener { setMode(Mode.SONGS) }
        binding.navAlbums.setOnClickListener { setMode(Mode.ALBUMS) }
        binding.navArtists.setOnClickListener { setMode(Mode.ARTISTS) }
        binding.navFolders.setOnClickListener { setMode(Mode.FOLDERS) }
        binding.navGenres.setOnClickListener { setMode(Mode.GENRES) }

        binding.seekBar.setOnSeekBarChangeListener(
            seekListener({ seekingMini = it }, binding.posText)
        )
        binding.npSeek.setOnSeekBarChangeListener(
            seekListener({ seekingNp = it }, binding.npPos)
        )

        ContextCompat.registerReceiver(
            this,
            updateReceiver,
            IntentFilter(PlayerService.ACTION_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        ensurePermissionsAndLoad()
    }

    override fun onDestroy() {
        unregisterReceiver(updateReceiver)
        super.onDestroy()
    }

    @Deprecated("Dipakai untuk kembali dari kategori dan layar Sedang Diputar")
    override fun onBackPressed() {
        when {
            binding.nowPlaying.visibility == View.VISIBLE ->
                binding.nowPlaying.visibility = View.GONE
            mode == Mode.CATEGORY -> setMode(lastTab)
            else -> super.onBackPressed()
        }
    }

    // ------------------------------------------------------------ mode & daftar

    private fun setMode(newMode: Mode) {
        mode = newMode
        if (newMode != Mode.CATEGORY) {
            lastTab = newMode
        }
        binding.categoryTitle.visibility = View.GONE
        binding.btnSort.visibility = if (newMode == Mode.SONGS) View.VISIBLE else View.GONE
        paintNav()
        val query = binding.searchBox.text?.toString() ?: ""
        when (newMode) {
            Mode.SONGS -> {
                binding.songList.layoutManager = LinearLayoutManager(this)
                binding.songList.adapter = songAdapter
                songAdapter.submit(sortedSongs(allSongs))
                songAdapter.filter(query)
            }
            Mode.ALBUMS -> {
                binding.songList.layoutManager = GridLayoutManager(this, 3)
                binding.songList.adapter = albumAdapter
                albumAdapter.submit(albumItems())
                albumAdapter.filter(query)
            }
            Mode.ARTISTS -> {
                binding.songList.layoutManager = LinearLayoutManager(this)
                binding.songList.adapter = categoryAdapter
                categoryAdapter.submit(artistItems())
                categoryAdapter.filter(query)
            }
            Mode.FOLDERS -> {
                binding.songList.layoutManager = LinearLayoutManager(this)
                binding.songList.adapter = categoryAdapter
                categoryAdapter.submit(folderItems())
                categoryAdapter.filter(query)
            }
            Mode.GENRES -> {
                binding.songList.layoutManager = LinearLayoutManager(this)
                binding.songList.adapter = categoryAdapter
                categoryAdapter.submit(genreItems())
                categoryAdapter.filter(query)
            }
            Mode.CATEGORY -> Unit
        }
    }

    private fun paintNav() {
        val active = 0xFFFFFFFF.toInt()
        val inactive = 0xFF8E8E9E.toInt()
        val tabs = listOf(
            binding.navSongs to Mode.SONGS,
            binding.navAlbums to Mode.ALBUMS,
            binding.navArtists to Mode.ARTISTS,
            binding.navFolders to Mode.FOLDERS,
            binding.navGenres to Mode.GENRES
        )
        val effective = if (mode == Mode.CATEGORY) categorySource else mode
        for ((view, tabMode) in tabs) {
            val isActive = tabMode == effective
            view.setTextColor(if (isActive) active else inactive)
            view.typeface = if (isActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun applyFilter(query: String) {
        when (mode) {
            Mode.SONGS, Mode.CATEGORY -> songAdapter.filter(query)
            Mode.ALBUMS -> albumAdapter.filter(query)
            else -> categoryAdapter.filter(query)
        }
    }

    private fun albumItems(): List<CategoryItem> =
        allSongs.groupBy { it.albumId }
            .map { (albumId, songs) ->
                val first = songs.first()
                CategoryItem(
                    key = albumId.toString(),
                    name = first.album.ifBlank { getString(R.string.album_unnamed) },
                    sub = first.artist,
                    count = songs.size,
                    albumId = albumId
                )
            }
            .sortedBy { it.name.lowercase() }

    private fun artistItems(): List<CategoryItem> =
        allSongs.groupBy { it.artist }
            .map { (artist, songs) ->
                CategoryItem(key = artist, name = artist, sub = "", count = songs.size)
            }
            .sortedBy { it.name.lowercase() }

    private fun folderItems(): List<CategoryItem> =
        allSongs.groupBy { it.folderPath }
            .map { (folderPath, songs) ->
                CategoryItem(
                    key = folderPath,
                    name = folderPath.substringAfterLast('/').ifBlank { folderPath },
                    sub = folderPath,
                    count = songs.size
                )
            }
            .sortedBy { it.name.lowercase() }

    private fun genreItems(): List<CategoryItem> =
        allSongs.groupBy { genreKey(it) }
            .map { (key, songs) ->
                CategoryItem(
                    key = key,
                    name = if (key == GENRE_UNKNOWN) getString(R.string.genre_unknown) else key,
                    sub = "",
                    count = songs.size
                )
            }
            .sortedBy { it.name.lowercase() }

    private fun genreKey(song: Song): String {
        val g = song.genre.trim()
        return if (g.isEmpty() || g.equals("<unknown>", ignoreCase = true)) GENRE_UNKNOWN else g
    }

    private fun openCategory(item: CategoryItem) {
        categorySource = mode
        val subset = when (mode) {
            Mode.ALBUMS -> allSongs.filter { it.albumId.toString() == item.key }
            Mode.ARTISTS -> allSongs.filter { it.artist == item.key }
            Mode.FOLDERS -> allSongs.filter { it.folderPath == item.key }
            Mode.GENRES -> allSongs.filter { genreKey(it) == item.key }
            else -> emptyList()
        }.sortedBy { it.title.lowercase() }
        mode = Mode.CATEGORY
        binding.songList.layoutManager = LinearLayoutManager(this)
        binding.songList.adapter = songAdapter
        songAdapter.submit(subset)
        songAdapter.filter(binding.searchBox.text?.toString() ?: "")
        binding.categoryTitle.text = item.name
        binding.categoryTitle.visibility = View.VISIBLE
        binding.btnSort.visibility = View.GONE
        paintNav()
    }

    private fun sortedSongs(songs: List<Song>): List<Song> = when (sortMode) {
        SortMode.TITLE -> songs.sortedBy { it.title.lowercase() }
        SortMode.ARTIST -> songs.sortedWith(
            compareBy({ it.artist.lowercase() }, { it.title.lowercase() })
        )
        SortMode.ALBUM -> songs.sortedWith(
            compareBy({ it.album.lowercase() }, { it.title.lowercase() })
        )
        SortMode.NEWEST -> songs.sortedByDescending { it.dateAddedSec }
        SortMode.LONGEST -> songs.sortedByDescending { it.durationMs }
    }

    private fun showSortDialog() {
        val modes = SortMode.values()
        val labels = arrayOf(
            getString(R.string.sort_by_title),
            getString(R.string.sort_by_artist),
            getString(R.string.sort_by_album),
            getString(R.string.sort_by_newest),
            getString(R.string.sort_by_longest)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.sort_title)
            .setSingleChoiceItems(labels, modes.indexOf(sortMode)) { dialog, which ->
                sortMode = modes[which]
                dialog.dismiss()
                if (mode == Mode.SONGS) {
                    setMode(Mode.SONGS)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ---------------------------------------------------------- sedang diputar

    private fun openNowPlaying() {
        if (npSongId < 0L) return
        binding.nowPlaying.visibility = View.VISIBLE
    }

    private fun onSongChangedForNp(songId: Long) {
        val song = allSongs.firstOrNull { it.id == songId } ?: return
        binding.npAlbum.text = song.album
        ArtworkLoader.loadInto(this, song.albumId, binding.npArt)
        loadLyrics(song)
    }

    /**
     * Lirik dari berkas pendamping .lrc/.txt di samping berkas lagu.
     * Lirik tertanam di dalam tag dibaca pada batch editor tag nanti.
     */
    private fun loadLyrics(song: Song) {
        currentLyrics = null
        if (lyricsVisible) {
            binding.npLyrics.text = getString(R.string.lyrics_none)
        }
        val path = song.path
        if (path.isBlank()) return
        Thread {
            val base = path.substringBeforeLast('.', "")
            val text = listOf("$base.lrc", "$base.txt")
                .map { File(it) }
                .firstOrNull { it.isFile }
                ?.let { runCatching { it.readText().take(20_000) }.getOrNull() }
                ?.trim()
                ?.ifEmpty { null }
            runOnUiThread {
                if (npSongId == song.id) {
                    currentLyrics = text
                    if (lyricsVisible) {
                        binding.npLyrics.text = text ?: getString(R.string.lyrics_none)
                    }
                }
            }
        }.start()
    }

    // ------------------------------------------------------------------ dialog

    private fun updateModeButtons(shuffle: Boolean, repeat: Int, sleepLeftSec: Int) {
        val activeColor = 0xFFFFD54F.toInt()
        val normalColor = 0xFFFFFFFF.toInt()
        val shuffleText = getString(if (shuffle) R.string.shuffle_on else R.string.shuffle)
        val shuffleColor = if (shuffle) activeColor else normalColor
        binding.btnShuffle.text = shuffleText
        binding.btnShuffle.setTextColor(shuffleColor)
        binding.npShuffle.text = shuffleText
        binding.npShuffle.setTextColor(shuffleColor)
        val repeatText = getString(
            when (repeat) {
                PlayerService.REPEAT_ALL -> R.string.repeat_all
                PlayerService.REPEAT_ONE -> R.string.repeat_one
                else -> R.string.repeat
            }
        )
        val repeatColor =
            if (repeat != PlayerService.REPEAT_OFF) activeColor else normalColor
        binding.btnRepeat.text = repeatText
        binding.btnRepeat.setTextColor(repeatColor)
        binding.npRepeat.text = repeatText
        binding.npRepeat.setTextColor(repeatColor)
        val sleepText = if (sleepLeftSec > 0) {
            getString(R.string.sleep_on, formatDuration(sleepLeftSec * 1000L))
        } else {
            getString(R.string.sleep_timer)
        }
        binding.btnSleep.text = sleepText
        binding.npSleep.text = sleepText
    }

    private fun showQueueDialog() {
        val songs = PlaybackState.playlist
        val order = PlaybackState.order
        val from = PlaybackState.orderPos
        if (songs.isEmpty() || order.isEmpty() || from < 0) {
            Toast.makeText(this, R.string.queue_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val positions = (from..order.lastIndex).toList()
        val labels = positions.map { pos ->
            val song = songs[order[pos]]
            val marker = if (pos == from) "▶ " else ""
            "$marker${song.title} — ${song.artist}"
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.queue_title)
            .setItems(labels) { _, which ->
                val song = songs[order[positions[which]]]
                sendCommand(PlayerService.ACTION_PLAY_ID) {
                    putExtra(PlayerService.EXTRA_SONG_ID, song.id)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showSleepDialog() {
        val minutes = intArrayOf(0, 5, 10, 15, 30, 45, 60)
        val labels = arrayOf(
            getString(R.string.sleep_off),
            "5 menit", "10 menit", "15 menit", "30 menit", "45 menit", "60 menit"
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.sleep_title)
            .setItems(labels) { _, which ->
                sendCommand(PlayerService.ACTION_SLEEP_TIMER) {
                    putExtra(PlayerService.EXTRA_MINUTES, minutes[which])
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // -------------------------------------------------------------------- lain

    private fun seekListener(setSeeking: (Boolean) -> Unit, label: TextView) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    label.text = formatDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                setSeeking(true)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                setSeeking(false)
                val target = seekBar?.progress ?: 0
                sendCommand(PlayerService.ACTION_SEEK) {
                    putExtra(PlayerService.EXTRA_POS, target)
                }
            }
        }

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun ensurePermissionsAndLoad() {
        val audio = audioPermission()
        if (ContextCompat.checkSelfPermission(this, audio) == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
            return
        }
        val toRequest = mutableListOf(audio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            toRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(toRequest.toTypedArray())
    }

    private fun loadSongs() {
        Thread {
            val songs = SongLoader.load(this)
            runOnUiThread {
                allSongs = songs
                setMode(Mode.SONGS)
                if (songs.isEmpty()) {
                    binding.emptyText.setText(R.string.empty_no_songs)
                    binding.emptyText.visibility = View.VISIBLE
                } else {
                    binding.emptyText.visibility = View.GONE
                }
            }
        }.start()
    }

    private fun sendCommand(action: String, extras: Intent.() -> Unit) {
        val intent = Intent(this, PlayerService::class.java).setAction(action)
        intent.extras()
        ContextCompat.startForegroundService(this, intent)
    }

    private companion object {
        const val GENRE_UNKNOWN = "__unknown__"
    }
}
