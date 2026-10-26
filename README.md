# Tasirin Music

Pemutar musik offline sederhana untuk Android. Memutar berkas audio yang tersimpan di perangkat, tanpa akun dan tanpa internet.

Repo ini juga menyimpan [ANALISIS.md](ANALISIS.md): hasil bedah sebuah APK yang namanya menyerupai Musicolet tetapi isinya ternyata aplikasi lain. Aplikasi di repo ini adalah karya asli yang terinspirasi kebutuhan yang sama (memutar musik lokal), bukan salinan kode aplikasi mana pun.

## Fitur v1.0

- Daftar semua lagu dari penyimpanan perangkat (MediaStore), urut judul.
- Pencarian berdasarkan judul atau artis.
- Putar, jeda, lagu berikutnya, lagu sebelumnya, dan geser posisi (seek bar).
- Pemutaran berlanjut di latar lewat layanan foreground dengan notifikasi berisi tombol kontrol.
- Lagu berlanjut otomatis ke lagu berikutnya setelah selesai.
- Fokus audio: pemutaran berhenti sejenak saat aplikasi lain mengambil alih audio.

## Teknologi

- Kotlin, minSdk 24, targetSdk 34.
- Antarmuka klasik Android View dengan ViewBinding dan RecyclerView.
- Pemutaran memakai MediaPlayer bawaan Android di dalam foreground service.
- Build sepenuhnya lewat GitHub Actions (`.github/workflows/build.yml`), menghasilkan artefak `tasirin-music-debug` berisi APK debug.

## Cara mengambil APK

1. Buka tab Actions di repo ini.
2. Pilih run terbaru yang hijau pada workflow "Build APK".
3. Unduh artefak `tasirin-music-debug`, lalu pasang `app-debug.apk` di perangkat (APK debug, perlu mengizinkan pasang dari sumber tidak dikenal).

## Peta jalan

- Antrean putar dan mode acak/ulang.
- Tab artis dan album.
- Ekualiser dan pengatur waktu tidur.
- Sampul album dari metadata berkas.
