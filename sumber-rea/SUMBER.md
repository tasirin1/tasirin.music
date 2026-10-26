# Hasil Pengambilan Sumber APK lewat REA

Tanggal: 2026-10-26
Target: `uptodown-in.krosbits.musicolet_1_i02o.apk` (SHA-256 `dc885921fc2d141a53906675bd555f6f5d3705119f37f892d3edf3e61b342550`)
Alat: REA MCP 6.8.0 dengan provider JADX (jadx-headless-mcp 0.7.1, JAR SHA-256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`) dan apktool 3.0.3, berjalan di JDK Temurin 17.

Pengambilan ini melengkapi [ANALISIS.md](../ANALISIS.md): kali ini isi APK diambil sampai level kelas dan kode sumber, bukan hanya manifes.

## Yang berhasil diambil

### 1. Paket dan komponen (REA `inspect_android_package`)

- Paket `com.uptodown`, nama "Uptodown App Store", versi 7.41 (741), minSdk 23, targetSdk 36.
- Komponen terhitung lengkap 94: **64 aktivitas, 15 layanan, 11 receiver, 4 provider** — angka yang sama persis dengan hitungan Androguard sebelumnya, jadi dua mesin bedah saling menguatkan.
- Daftar 32 izin sama dengan temuan sebelumnya.

### 2. Semua kelas (REA `search_android_classes`, kueri kosong)

- Total **13.961 kelas** terinventarisasi penuh. Daftar lengkapnya ada di berkas `kelas-semua.txt` di folder ini (539 KB).
- Hanya **109 kelas** yang masih memakai nama paket `com.uptodown.*` (daftarnya di `kelas-uptodown.txt`). Sisanya nama pendek hasil pengaburan (obfuscation) R8, terbanyak di paket `defpackage`.
- Paket pustaka terbesar: `androidx.compose` 4.810 kelas — masuk akal untuk aplikasi yang antarmukanya memakai Jetpack Compose; disusul `com.google` 543 dan `androidx.core` 393.

### 3. Struktur kelas (REA `inspect_android_class`)

Contoh pada `com.uptodown.activities.MainActivity`: **81 metode dan 40 field**. Sebagian besar nama metodenya sudah teraburkan menjadi nama pendek seperti `A0`, `B1`, `C0` — hanya nama yang diwajibkan Android (mis. `onCreate`) yang masih asli.

### 4. Kode sumber hasil dekompilasi (REA `inspect_android_method`)

- Metode `MainActivity.onCreate(Bundle)` berhasil diambil sebagai **sumber Java hasil dekompilasi JADX**, 12.636 karakter, tanpa fallback ke smali.
- Berkas lengkapnya: `oncreate-mainactivity.java.txt` di folder ini.
- Ciri khas hasil dekompilasi aplikasi yang teraburkan: rujukan ke kelas seperti `defpackage.ml`, pewarisan berlapis yang ditulis ulang JADX sebagai komentar `@Override`, dan peringatan JADX soal inferensi tipe. Isinya tetap terbaca sebagai logika program, bukan kode asli persis seperti yang ditulis pengembangnya.

### 5. Sumber daya (REA `decode_android_resources`, apktool)

- **2.433 berkas** terdekode dari sumber daya APK.
- Manifes hasil dekode (XML terbaca penuh, 41.581 karakter) ada di berkas `manifest.xml` di folder ini.
- String sumber daya terbaca (nama + nilai), dan aplikasi ini membawa **90 lokal bahasa**, termasuk `in` dan `in-rID` (Bahasa Indonesia).

### 6. Tanda tangan (REA `inspect_apk_signature`, dari sesi sebelumnya)

- Skema v1, v2, v3 lengkap, satu penandatangan yang sama: Uptodown Technologies SL, SHA-256 sertifikat `822b9ca12b534ebcf426632221d951bfc60eb08f9f0cf2839c321b0685c2e8a4`.
- Catatan kejujuran alat: REA membaca deklarasi tanda tangan; verifikasi kriptografis tidak dilakukan oleh REA maupun JADX (`signature_verification: not_performed`).

## Yang belum diambil

- Sumber semua 13.961 kelas satu per satu: secara teknis bisa (panggil `inspect_android_method` per metode), tetapi volumenya sangat besar. Yang diambil sebagai bukti kemampuan adalah struktur satu kelas kunci dan sumber lengkap satu metodenya. Kelas atau metode lain bisa diambil sesuai permintaan.
- Berkas biner mentah di dalam APK (DEX, pustaka `.so`) tidak disalin ke repo ini; yang disimpan adalah hasil bedahnya.

## Isi folder ini

- `SUMBER.md` — laporan ini.
- `kelas-semua.txt` — daftar 13.961 nama kelas.
- `kelas-uptodown.txt` — 109 kelas bernama paket `com.uptodown.*`.
- `manifest.xml` — AndroidManifest hasil dekode apktool.
- `oncreate-mainactivity.java.txt` — sumber Java hasil dekompilasi `MainActivity.onCreate`.
