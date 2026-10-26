# Menyiapkan workflow build (satu kali, manual)

Berkas workflow GitHub Actions belum bisa dipasang oleh agen karena integrasi GitHub yang dipakai agen tidak mempunyai izin menulis ke folder `.github/workflows`. Isi workflow-nya sudah jadi di bawah ini. Pasang satu kali lewat situs GitHub:

1. Buka repo ini di github.com, klik **Add file → Create new file**.
2. Nama berkas: `.github/workflows/build.yml`
3. Tempel seluruh isi blok YAML di bawah ini.
4. Klik **Commit changes**.

Sesudah itu setiap push ke `main` menjalankan workflow "Build APK" dan menghasilkan artefak `tasirin-music-debug` berisi `app-debug.apk`.

```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    name: Assemble debug APK
    runs-on: ubuntu-24.04
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle 8.7
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.7'

      - name: Assemble debug APK
        run: gradle :app:assembleDebug --stacktrace

      - name: Upload APK artifact
        uses: actions/upload-artifact@v4
        with:
          name: tasirin-music-debug
          path: app/build/outputs/apk/debug/app-debug.apk
          if-no-files-found: error
```

Setelah berkas workflow terpasang dan run pertamanya selesai, berkas `WORKFLOW.md` ini boleh dihapus.
