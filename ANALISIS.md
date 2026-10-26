# Analisis APK `uptodown-in.krosbits.musicolet_1_i02o.apk`

Tanggal analisis: 2026-10-26
Alat: REA MCP 6.8.0 (inspeksi tanda tangan), Androguard 4.1.4 (manifes dan komponen), pemeriksaan isi ZIP dan string DEX.
Berkas: 15 MB, SHA-256 `dc885921fc2d141a53906675bd555f6f5d3705119f37f892d3edf3e61b342550`

## Kesimpulan utama

**Isi berkas ini BUKAN Musicolet.** Dari namanya berkas ini tampak seperti pemutar musik Musicolet dari Krosbits, tetapi manifes di dalamnya menyatakan aplikasi lain: **Uptodown App Store 7.41**, paket `com.uptodown`, ditandatangani oleh Uptodown Technologies SL. Tidak ada satu pun jejak Musicolet atau Krosbits di dalam berkas ini.

## Fakta (terverifikasi)

### Identitas aplikasi

- Nama aplikasi: **Uptodown App Store**
- Paket: `com.uptodown`
- Versi: 7.41 (versionCode 741)
- minSdk 23 (Android 6.0), targetSdk 36
- Aktivitas utama: `com.uptodown.activities.MainActivity`

### Nol jejak Musicolet

- Pencarian string `musicolet` di keempat berkas DEX: **0 hasil**.
- Pencarian string `krosbits`: **0 hasil**.
- Tidak ada komponen (aktivitas, layanan, receiver, provider) yang berasal dari paket Musicolet.

### Penandatangan

- Penandatangan: **Uptodown Technologies SL**, Málaga, Spanyol (sertifikat self-signed, penerbit sama dengan subjek).
- SHA-256 sertifikat: `822b9ca12b534ebcf426632221d951bfc60eb08f9f0cf2839c321b0685c2e8a4` — hasil REA dan Androguard sama persis.
- Masa berlaku sertifikat: 2024-05-02 sampai 2051-09-18.
- Skema tanda tangan v1, v2, dan v3 semuanya ada, masing-masing satu penandatangan yang sama (temuan REA). Catatan batas REA: REA membaca deklarasi tanda tangan dan tidak memverifikasi kriptografinya; sertifikat di atas diekstrak Androguard dari blok penandatanganan v2.

### Struktur berkas

- 2.169 berkas di dalam APK, total terurai sekitar 32,5 MB.
- 4 berkas DEX (multidex): `classes.dex` 8,7 MB, `classes2.dex` 0,55 MB, `classes3.dex` 7,5 MB, `classes4.dex` 7,4 MB.
- Pustaka native untuk 4 ABI (arm64-v8a, armeabi-v7a, x86, x86_64), termasuk pustaka milik Uptodown sendiri: `libuptodown-native.so` dan `libutd-services-native.so`, plus `libandroidx.graphics.path.so` dan `libdatastore_shared_counter.so`.
- Dibangun dengan Android Gradle Plugin 9.3.1 (dari `app-metadata.properties`).

### Komponen

- 64 aktivitas, 59 di antaranya milik `com.uptodown` sendiri. Contoh layarnya: AppDetailActivity, AppFilesActivity, MyApps, MyDownloads, OldVersionsActivity, FreeUpSpaceActivity, FeedActivity, LoginActivity, GdprPrivacySettings.
- 15 layanan, termasuk FirebaseMessagingService dan layanan WorkManager.
- 11 receiver, termasuk BootDeviceReceiver milik Uptodown.
- 4 provider, termasuk FileProvider dan FirebaseInitProvider.

### Izin (32 izin)

Kelompok yang paling menonjol adalah izin khas toko aplikasi:

- Pemasangan dan penghapusan aplikasi: `INSTALL_PACKAGES`, `REQUEST_INSTALL_PACKAGES`, `UPDATE_PACKAGES_WITHOUT_USER_ACTION`, `DELETE_PACKAGES`, `REQUEST_DELETE_PACKAGES`.
- Melihat semua aplikasi terpasang: `QUERY_ALL_PACKAGES`, `PACKAGE_USAGE_STATS`, `GET_PACKAGE_SIZE`.
- Berkas dan unduhan: `MANAGE_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`.
- Iklan dan atribusi: `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`.
- Lainnya yang menarik: `ACCESS_SUPERUSER`, `RECORD_AUDIO`, `GET_ACCOUNTS`, `USE_BIOMETRIC`, `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `INTERNET`.

### Tumpukan teknologi

- Jetpack Compose dengan Material3, AppCompat, ViewBinding (106 penanda versi pustaka AndroidX di META-INF).
- Firebase: Messaging (notifikasi push), Measurement/Analytics, Sessions.
- Google Sign-In dan Credential Manager.
- Room, WorkManager, DataStore.
- Picasso untuk memuat gambar.
- InMobi CMP (aktivitas persetujuan iklan) dan layar pengaturan privasi GDPR milik Uptodown.
- Mendeklarasikan fitur `android.software.leanback` (dukungan Android TV).

## Tebakan (belum terbukti, diberi label jujur)

1. **Asal berkas.** Berkas ini kemungkinan adalah APK aplikasi toko Uptodown yang ikut terunduh saat mengunduh Musicolet dari situs Uptodown, atau berkas unduhan yang namanya tertukar di perangkat. Pola nama `uptodown-<nama aplikasi target>` cocok dengan penamaan unduhan dari situs tersebut. Ini dugaan: yang pasti hanya bahwa isinya aplikasi toko Uptodown, bukan Musicolet.
2. **`ACCESS_SUPERUSER`.** Kemungkinan dipakai untuk mendeteksi perangkat yang di-root (pola umum aplikasi toko dan keamanan), bukan untuk memakai akses root. Perlu bedah kode untuk memastikan.
3. **`RECORD_AUDIO` dan fitur mikrofon/kamera.** Kemungkinan untuk fitur pencarian suara atau kebutuhan komponen pihak ketiga di dalam aplikasi. Belum ada bukti pemakaiannya di kode.
4. **Pustaka native `libutd-services-native.so`.** Dari namanya, kemungkinan menangani layanan unduh/pasang dan pemeriksaan integritas milik Uptodown. Isi fungsinya belum dibedah.
5. **Model pendapatan.** Komponen InMobi CMP dan izin layanan iklan menguatkan dugaan bahwa aplikasi toko ini menampilkan iklan. Ini dugaan berbasis komponen, bukan pengamatan saat aplikasi berjalan.

## Batas analisis ini

- Alat REA untuk bedah paket Android (`inspect_android_package`, berbasis JADX) dan tata letak biner (`inspect_binary_layout`, berbasis pwntools) belum tersedia di mesin analisis ini, jadi isi kode DEX belum dibaca per kelas. Analisis manifes dan komponen memakai Androguard.
- Tidak ada pengamatan runtime: aplikasi tidak dipasang dan tidak dijalankan.
- Langkah lanjut yang paling bernilai bila ingin kepastian soal tebakan di atas: pasang provider JADX untuk REA, lalu telusuri pemakaian `ACCESS_SUPERUSER`, `RECORD_AUDIO`, dan pustaka native Uptodown langsung di kode.
