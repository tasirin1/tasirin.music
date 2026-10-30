# Tasirin Music

Pemutar musik offline sederhana untuk Android. Memutar berkas audio yang tersimpan di perangkat, tanpa akun dan tanpa internet.

Repo ini juga menyimpan dua dokumen bedah APK:

- [ANALISIS.md](ANALISIS.md): bedah sebuah APK yang namanya menyerupai Musicolet tetapi isinya ternyata aplikasi lain.
- [ANALISIS-MUSICOLET.md](ANALISIS-MUSICOLET.md): bedah Musicolet 6.7.3 asli (manifes, izin, komponen, changelog) yang menjadi peta fitur tiruan proyek ini.

Aplikasi di repo ini adalah karya asli yang terinspirasi kebutuhan yang sama (memutar musik lokal), bukan salinan kode aplikasi mana pun.

## Fitur v1.2

Menyesuaikan struktur dan tampilan dengan Musicolet:

- Tema gelap penuh, seperti tampilan bawaan Musicolet.
- Bilah navigasi bawah: **Lagu, Album, Artis, Folder, Genre**.
- Album tampil sebagai kotak-kotak sampul (grid); ketuk album/artis/folder/genre untuk membuka daftar lagunya, dan lagu diputar dalam konteks itu (antrean mengikuti isi kategori).
- Layar **Sedang Diputar** layar penuh: sampul besar, judul/artis/album, geser posisi, kontrol putar, acak/ulang/antrean/tidur, dan panel **lirik** dari berkas pendamping `.lrc`/`.txt` di samping lagu.
- Pilihan urutan sortir daftar lagu: judul, artis, album, baru ditambahkan, durasi terpanjang.

## Fitur v1.1

- Semua fitur v1.0 di bawah.
- Mode **acak** dan **ulang** (mati / semua lagu / satu lagu).
- **Antrean putar**: lihat sisa antrean dan lompat ke lagu mana pun di dalamnya.
- **Pengatur waktu tidur**: pemutaran berhenti sendiri setelah 5–60 menit.
- **Sampul album** di daftar lagu dan di notifikasi pemutaran.
- **Sesi media (MediaSession)**: kontrol dari headset/Bluetooth dan layar kunci.
- **Ingat sesi terakhir**: lagu, posisi putar, mode acak dan ulang dipulihkan saat layanan dibuat lagi.

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
- Pemutaran memakai MediaPlayer bawaan Android di dalam foreground service, dibungkus MediaSession untuk kontrol eksternal.
- Build sepenuhnya lewat GitHub Actions (`.github/workflows/build.yml`), menghasilkan artefak `tasirin-music-debug` berisi APK debug.

## Cara mengambil APK

1. Buka tab Actions di repo ini.
2. Pilih run terbaru yang hijau pada workflow "Build APK".
3. Unduh artefak `tasirin-music-debug`, lalu pasang `app-debug.apk` di perangkat (APK debug, perlu mengizinkan pasang dari sumber tidak dikenal).

## Peta jalan

Mengikuti peta fitur di [ANALISIS-MUSICOLET.md](ANALISIS-MUSICOLET.md):

- Tab album, artis, dan folder; pilihan urutan sortir.
- Multi-antrean: beberapa antrean mandiri yang bisa berpindah tanpa kehilangan posisi.
- Playlist buatan pengguna.
- Editor tag dan lirik (termasuk lirik tersinkron).
- Ekualiser dan ReplayGain.
- Cadangan/pulih (backup/restore) dan daftar lagu paling sering diputar.
