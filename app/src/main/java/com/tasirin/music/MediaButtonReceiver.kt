package com.tasirin.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.core.content.ContextCompat

/**
 * Penerima tombol media (headset/Bluetooth) yang membangunkan PlayerService
 * saat proses aplikasi belum berjalan.
 *
 * Kelas MediaButtonReceiver bawaan androidx.media sudah dihapus pada
 * media 1.6+, jadi penerusan tombol dilakukan sendiri di sini: kode tombol
 * diterjemahkan menjadi aksi layanan yang setara. Saat layanan sudah jalan,
 * tombol yang sama tiba lewat callback MediaSession di PlayerService.
 */
class MediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MEDIA_BUTTON) return

        @Suppress("DEPRECATION")
        val event = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return
        if (event.action != KeyEvent.ACTION_DOWN) return

        val action = when (event.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> PlayerService.ACTION_PLAY
            KeyEvent.KEYCODE_MEDIA_PAUSE -> PlayerService.ACTION_PAUSE
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK -> PlayerService.ACTION_TOGGLE
            KeyEvent.KEYCODE_MEDIA_NEXT -> PlayerService.ACTION_NEXT
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> PlayerService.ACTION_PREV
            KeyEvent.KEYCODE_MEDIA_STOP -> PlayerService.ACTION_STOP
            else -> return
        }
        val serviceIntent = Intent(context, PlayerService::class.java).setAction(action)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
