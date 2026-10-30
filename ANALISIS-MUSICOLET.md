# Analisis APK Musicolet 6.7.3 (build 422) — target tiruan Tasirin Music

Tanggal analisis: 2026-10-30
Alat: REA MCP 6.8.0 (JADX headless — inspeksi paket, manifes terdekode, inventaris komponen), pemeriksaan isi ZIP dan aset `change_log.txt` bawaan APK.
Berkas: `in.krosbits.musicolet_6.7.3_build422-42200_minAPI16_armeabi-v7a__nodpi__apkmirror.com.apk`, 11.352.506 byte, SHA-256 `2c4754b3d6d8d81bf3a291a08a0aa8b4b4d86bd2e21358a00a1d4bedd16fe2a4`.

Hasil mentah REA tersimpan di `sumber-rea/inspeksi-musicolet-6.7.3.json` dan manifes terdekode di `sumber-rea/manifest-musicolet-6.7.3.xml`.

## Kesimpulan utama

**Berkas ini benar-benar Musicolet** dari Krosbits (paket `in.krosbits.musicolet`) — berbeda dari berkas yang dianalisis pada 2026-10-26 (lihat `ANALISIS.md`), yang ternyata aplikasi lain berkedok nama Musicolet. Musicolet adalah pemutar musik **offline murni**: dari 17 izinnya **tidak ada izin INTERNET sama sekali**.

Dokumen ini adalah peta *perilaku/fitur* untuk ditiru Tasirin Music dengan kode orisinal. Tidak ada kode sumber Musicolet yang disalin; yang diambil hanya daftar kemampuan yang terlihat dari manifes, komponen, dan changelog resmi di dalam APK.

## Fakta (terverifikasi dari APK)

### Identitas

- Nama: Musicolet (Krosbits), paket `in.krosbits.musicolet`
- Versi: `6.7.3 build422`, versionCode 42200, minSdk 16, targetSdk 33
- Aktivitas utama: `in.krosbits.musicolet.MusicActivity`
- 4.770 kelas (2 berkas DEX), 2.204 berkas sumber daya

### Struktur & mesin audio

- Pustaka native FFmpeg lengkap (`libavcodec`, `libavformat`, `libavfilter`, `libavutil`, `libswresample`) + `lib_musicolet.so` milik sendiri → Musicolet memakai dekoder/berkas audio sendiri (changelog menyebut "Musicolet decoder"), bukan hanya MediaPlayer bawaan. Ini fondasi fitur khasnya: putar banyak format, ReplayGain, equalizer banyak band, dan edit tag.
- Aset teks: `change_log.txt` (riwayat fitur resmi), `about.html`, `credits.html`.

### Izin (17) dan artinya

- Media: `READ_MEDIA_AUDIO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` (tulis untuk edit tag & berkas di Android lama).
- **Tidak ada `INTERNET`** — pemutar offline total.
- Audio: `MODIFY_AUDIO_SETTINGS` (equalizer), `BLUETOOTH` + `BLUETOOTH_CONNECT` (kontrol perangkat Bluetooth).
- Putar: `FOREGROUND_SERVICE`, `WAKE_LOCK`, `USE_FULL_SCREEN_INTENT` (pemutar layar kunci), `POST_NOTIFICATIONS`, `EXPAND_STATUS_BAR`.
- Sistem: `WRITE_SETTINGS` (mis. jadikan nada dering), `INSTALL_SHORTCUT` (pintasan lagu/playlist ke launcher).
- Komersial: `com.android.vending.BILLING` (pembelian versi Pro) + izin khusus `in.krosbits.musicolet.ALLOW_CAST_AUDIO` (Chromecast).

### Komponen → fitur (74 komponen; ±48 milik Krosbits)

| Komponen | Fitur yang ditunjukkan |
|---|---|
| `MusicActivity`, `MiniPlayerActivity`, `SearchActivity` | Layar utama, mini player, pencarian |
| `MusicService`, `MediaBrowserServiceImpl`, `QsService` | Mesin putar, Android Auto/media browser, tile Quick Settings |
| `RemoteMediaControlReceiver`, `BluetoothUsbReceiver`, `MediaMountedReceiver` | Tombol headset, Bluetooth/USB, kartu SD dipasang |
| `Tag2Activity`, `LyricsEditActivity` | Editor tag metadata & editor lirik |
| `EqualizerActivity2` | Equalizer bawaan |
| `RGReadCalcActivity`, `ReplayGainReadCalcService` | Hitung & terapkan ReplayGain (normalisasi volume) |
| `PlaylistActivity`, `PlaylistEditActivity` | Playlist pengguna |
| `FolderExcluderActivity`, `IncludedFoldersActivity` | Pilih/kecualikan folder yang dipindai |
| `MostPlayedActivity` | Daftar paling sering diputar (per bulan/tahun, dari changelog 6.7) |
| `RestoreActivity`, `DbCleanActivity`, `FileUtilsActivity` | Backup/restore (ZIP, changelog 6.0), bersih-bersih basis data, operasi berkas |
| `LockScreenActivity`, `LockScreenReceiver` | Pemutar layar kunci khas Musicolet |
| `Widget4X1`, `Widget4X1Adv`, `Widget4X3`, `WidgetService`, `WidgetPrefActivity` | Tiga widget + pengaturannya |
| `CastProvider` + komponen Google Cast | Putar ke Chromecast |
| `IapActivity` | Pembelian dalam aplikasi (Pro) |
| `SettingsActivity`, `WelcomeActivity`, `FAQActivity`, `AboutActivity` | Pengaturan, sambutan, FAQ, tentang |

### Fitur dari changelog resmi di dalam APK (kutipan inti)

- 6.7: daftar **Most Played** akhir bulan/tahun bisa dilihat/dibagikan.
- 6.5: **kecepatan putar berbeda per berkas audio**; opsi mulai putar saat aplikasi dibuka; opsi menonaktifkan Chromecast.
- 6.4: buat **lirik tersinkron** di Editor Tag; **Bookmark/Catatan** per lagu; pengatur tidur dengan **volume mengecil bertahap** di lagu terakhir.
- 6.3 [PRO]: **normalisasi volume ReplayGain** + hitung RG berkas apa pun.
- 6.2–6.1: equalizer dengan **preset per perangkat Bluetooth**; jumlah band bisa dipilih **3/5/10/15/31**.
- 6.0: backup/restore **satu berkas ZIP**; tab Antrean dengan geser-untuk-aksi; layar kunci bisa digeser naik untuk menutup.

## Peta fitur tiruan untuk Tasirin Music

Perilaku yang ditiru (kode tetap orisinal):

**Sudah ada — v1.0:** daftar lagu MediaStore urut judul; pencarian judul/artis; putar/jeda/berikutnya/sebelumnya/seek; layanan foreground + notifikasi kontrol; lanjut otomatis; fokus audio.

**Batch v1.1 (dikerjakan bersama analisis ini):**
- Mode **acak** dan **ulang** (mati / semua / satu lagu) — perilaku dasar Musicolet.
- **Antrean putar** yang bisa dilihat dan dilompati (tab Queue khas Musicolet, versi sederhana: dialog antrean).
- **Pengatur waktu tidur** (hentikan putar setelah N menit).
- **Sampul album** di daftar lagu dan notifikasi.
- **MediaSession**: kontrol headset/Bluetooth & layar kunci lewat sesi media standar (padanan perilaku `RemoteMediaControlReceiver` Musicolet).
- **Ingat sesi terakhir**: lagu, posisi, mode acak/ulang dipulihkan saat layanan dibuat lagi.

**Antrean batch berikut (belum dikerjakan):**
- Tab **Album / Artis / Folder** + urutan sortir beragam; **multi-antrean** khas Musicolet (beberapa antrean mandiri yang bisa berpindah tanpa kehilangan posisi masing-masing).
- **Playlist pengguna** (buat/ubah/hapus, tambah lagu).
- **Editor tag + lirik** (termasuk lirik tersinkron `.lrc`), sampul dari metadata berkas.
- **Equalizer**, **ReplayGain**, **kecepatan putar per lagu**, **bookmark/catatan**.
- **Backup/restore ZIP**, daftar **Paling Sering Diputar**, widget, tile Quick Settings.

## Batas analisis

- REA/JADX membaca manifes & inventaris kelas; isi metode per kelas tidak dibedah untuk dokumen ini (tidak diperlukan untuk meniru perilaku, dan kode asli tidak dipakai).
- Verifikasi tanda tangan penandatangan **tidak dilakukan** REA pada jalan ini (`signature_verification: not_performed`); identitas bertumpu pada nama paket, label, komponen, dan struktur internal yang konsisten dengan Musicolet resmi.
- Tidak ada pengamatan runtime: aplikasi tidak dipasang/dijalankan; perilaku fitur disimpulkan dari komponen + changelog resmi di dalam APK.
