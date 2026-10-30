package com.tasirin.music

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.tasirin.music.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SongAdapter
    private var seeking: Boolean = false
    private var currentPlaying: Boolean = false

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
            val index = intent.getIntExtra(PlayerService.EXTRA_INDEX, -1)
            if (index < 0) {
                binding.miniPlayer.visibility = View.GONE
                return
            }
            binding.miniPlayer.visibility = View.VISIBLE
            binding.nowTitle.text = intent.getStringExtra(PlayerService.EXTRA_TITLE) ?: ""
            binding.nowArtist.text = intent.getStringExtra(PlayerService.EXTRA_ARTIST) ?: ""
            currentPlaying = intent.getBooleanExtra(PlayerService.EXTRA_PLAYING, false)
            binding.btnPlayPause.setImageResource(
                if (currentPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            )
            val duration = intent.getIntExtra(PlayerService.EXTRA_DURATION, 0)
            val position = intent.getIntExtra(PlayerService.EXTRA_POSITION, 0)
            if (duration > 0) {
                binding.seekBar.max = duration
                binding.durText.text = formatDuration(duration.toLong())
            }
            if (!seeking) {
                binding.seekBar.progress = position
                binding.posText.text = formatDuration(position.toLong())
            }
            updateModeButtons(
                intent.getBooleanExtra(PlayerService.EXTRA_SHUFFLE, false),
                intent.getIntExtra(PlayerService.EXTRA_REPEAT, PlayerService.REPEAT_OFF),
                intent.getIntExtra(PlayerService.EXTRA_SLEEP_LEFT_SEC, -1)
            )
            adapter.setCurrent(index)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SongAdapter { shownPosition ->
            val originalIndex = adapter.originalIndexOf(shownPosition)
            sendCommand(PlayerService.ACTION_PLAY_INDEX) {
                putExtra(PlayerService.EXTRA_INDEX, originalIndex)
            }
        }
        binding.songList.layoutManager = LinearLayoutManager(this)
        binding.songList.adapter = adapter

        binding.searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnPlayPause.setOnClickListener { sendCommand(PlayerService.ACTION_TOGGLE) {} }
        binding.btnNext.setOnClickListener { sendCommand(PlayerService.ACTION_NEXT) {} }
        binding.btnPrev.setOnClickListener { sendCommand(PlayerService.ACTION_PREV) {} }
        binding.btnShuffle.setOnClickListener { sendCommand(PlayerService.ACTION_TOGGLE_SHUFFLE) {} }
        binding.btnRepeat.setOnClickListener { sendCommand(PlayerService.ACTION_CYCLE_REPEAT) {} }
        binding.btnQueue.setOnClickListener { showQueueDialog() }
        binding.btnSleep.setOnClickListener { showSleepDialog() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.posText.text = formatDuration(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                seeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seeking = false
                val target = seekBar?.progress ?: 0
                sendCommand(PlayerService.ACTION_SEEK) {
                    putExtra(PlayerService.EXTRA_POS, target)
                }
            }
        })

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

    private fun updateModeButtons(shuffle: Boolean, repeat: Int, sleepLeftSec: Int) {
        val activeColor = 0xFFFFD54F.toInt()
        val normalColor = 0xFFFFFFFF.toInt()
        binding.btnShuffle.text = getString(
            if (shuffle) R.string.shuffle_on else R.string.shuffle
        )
        binding.btnShuffle.setTextColor(if (shuffle) activeColor else normalColor)
        binding.btnRepeat.text = getString(
            when (repeat) {
                PlayerService.REPEAT_ALL -> R.string.repeat_all
                PlayerService.REPEAT_ONE -> R.string.repeat_one
                else -> R.string.repeat
            }
        )
        binding.btnRepeat.setTextColor(
            if (repeat != PlayerService.REPEAT_OFF) activeColor else normalColor
        )
        binding.btnSleep.text = if (sleepLeftSec > 0) {
            getString(R.string.sleep_on, formatDuration(sleepLeftSec * 1000L))
        } else {
            getString(R.string.sleep_timer)
        }
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
                adapter.submit(songs)
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
}
