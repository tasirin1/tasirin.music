package com.tasirin.music

/**
 * Keadaan antrean putar yang dibagikan PlayerService ke antarmuka.
 *
 * Layanan dan aktivitas berjalan di proses yang sama, jadi keadaan ini cukup
 * disimpan sebagai objek bersama di memori. PlayerService adalah satu-satunya
 * penulis; antarmuka hanya membaca (mis. untuk menampilkan dialog antrean).
 */
object PlaybackState {
    @Volatile
    var playlist: List<Song> = emptyList()

    /** Indeks ke [playlist], tersusun menurut urutan putar aktif (termasuk acak). */
    @Volatile
    var order: List<Int> = emptyList()

    /** Posisi lagu yang sedang diputar di dalam [order]. */
    @Volatile
    var orderPos: Int = -1

    @Volatile
    var currentSongId: Long = -1L
}
