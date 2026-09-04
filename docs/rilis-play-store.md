# Rilis ke Play Store (proses update v1.7)

Bukan dokumen per-fitur — ini catatan proses rilis yang sedang berjalan per
2026-09-04, supaya bisa dilanjutkan tanpa mengulang dari nol.

## 1. Status saat ini

- App sudah live di Play Store, versi terakhir yang ter-publish: **versionCode
  6 / versionName "1.6"**.
- Upload key lama (`G:\My Drive\05-Archive\PERS - KEY\keystore.jks`, alias
  `key0`) **passwordnya hilang/lupa** — tidak ada di Windows Credential
  Manager mesin manapun yang sempat dicek, dan tidak diketahui pernah dipakai
  di PC lain.
- Sudah dikonfirmasi lewat percobaan upload ke Play Console: sertifikat yang
  Play Store harapkan (SHA1 `62:98:68:3D:21:62:F6:29:F9:37:82:B5:89:29:B9:8E:
  0B:05:27:BA`) memang persis sertifikat dari `keystore.jks` lama itu — bukan
  salah file.
- App ini terdaftar di **Play App Signing** ("Protect app signing key: Releases
  signed by Play" — terlihat di Play Console → app → **Protected with Play**),
  jadi opsi **Request upload key reset** tersedia (kalau tidak terdaftar,
  opsi ini tidak akan ada dan jalurnya beda — harus kontak Play Support).
- **Sudah submit "Request upload key reset"** dengan alasan "I forgot the
  password to my keystore", termasuk upload `upload_certificate.pem` (lihat
  §2). Status: **pending review Google** (cek ulang statusnya di URL yang
  sama: Play Console → app → Protected with Play → klik baris "Protect app
  signing key" → halaman "App signing" → section "Request upload key reset").
- Selama pending, upload AAB dengan key baru **akan ditolak** — sudah
  dicoba & gagal sekali (percobaan upload ke Open Testing, error "signed
  with the wrong key"), itu wajar dan memang belum bisa sampai request
  di-approve.

## 2. Keystore baru (upload key pengganti)

- Path: `G:\My Drive\05-Archive\PERS - KEY\keystore-upload-2026.jks`
- Alias: `upload`
- Password: **tersimpan di password manager** — sengaja tidak dicatat di sini
  supaya dokumen ini aman untuk di-commit ke git. Kalau lupa lagi, satu-
  satunya jalan adalah generate keystore baru lagi dan ulangi proses
  §4 dari awal (request reset lagi ke Google).
- Sertifikat publik yang sudah diserahkan ke Google: `G:\My Drive\05-Archive\
  PERS - KEY\upload_certificate.pem` (SHA256 `BF:9B:9B:E2:FD:0C:26:24:CD:D5:
  A9:9F:EF:97:72:41:C9:42:ED:31:28:F8:6F:17:C9:6C:72:5A:27:D8:36:7C`).
- **TODO setelah approved**: backup `keystore-upload-2026.jks` ke tempat lain
  juga (bukan cuma Google Drive), supaya kejadian password hilang tidak
  terulang untuk key yang ini.

## 3. Setup teknis di repo (sudah di-wiring, siap pakai)

- `app/build.gradle`: `signingConfigs.release` baca dari `keystore.properties`
  di root project (`alkaukabaandroid/keystore.properties`) — file ini **tidak
  di-commit** (ada di `.gitignore`, sama seperti `*.jks`).
- Template ada di `keystore.properties.template` (boleh di-commit, tidak
  berisi rahasia). Kalau perlu setup ulang di mesin lain: copy jadi
  `keystore.properties`, isi 4 baris (`storeFile`, `storePassword`,
  `keyAlias`, `keyPassword`) — **jangan pernah isi file `.template` itu
  langsung**, itu bukan file yang di-gitignore.
- `versionCode` sudah dinaikkan **6 → 7**, `versionName` **1.6 → 1.7**.
- Build command:
  ```
  JAVA_HOME="C:/Program Files/Java/jdk-17.0.2" ANDROID_HOME="C:/Users/<user>/AppData/Local/Android/Sdk" ./gradlew.bat bundleRelease --console=plain
  ```
- Output AAB signed (versionCode 7): `app/build/outputs/bundle/release/
  app-release.aab` — sudah di-build & diverifikasi sertifikatnya cocok
  dengan `keystore-upload-2026.jks`. Tinggal upload ulang setelah request
  reset di §1 di-approve.

## 4. Langkah lanjutan begitu request upload key reset di-approve

1. Cek statusnya di Play Console (lokasi sama seperti §1) — dari "pending"
   jadi approved/rejected. Biasanya ada notifikasi juga.
2. Kalau approved: buka **Test and release → Production → Create new
   release**, upload `app-release.aab` yang sudah ada (tidak perlu build
   ulang kecuali ada perubahan kode baru).
3. Isi release notes (draft sudah ada di §5).
4. Preview & confirm → rollout ke production.
5. (Opsional) upload juga ke track Open Testing kalau memang dipakai untuk
   QA sebelum production.

## 5. Draft release notes v1.7

Ringkasan dari 33 commit sejak versionCode 6 (`git log 493caed..HEAD`).

**"What's new" (Bahasa Indonesia, ≤500 karakter, siap paste ke Play
Console):**

```
Update besar v1.7:
• Desain baru navy & gold di seluruh aplikasi (Beranda, Waktu Sholat, Bulan Hijriyah, Gerhana, Kalender, Kiblat, Profil)
• Fitur baru: Gerhana — daftar gerhana Bulan & Matahari terdekat dari lokasimu
• Fitur baru: halaman Profil (akun & keamanan)
• Kompas Kiblat kini menampilkan arah Matahari & bayangan real-time
• Perbaikan akurasi rumus hisab Waktu Sholat & Awal Bulan
• Berbagai perbaikan bug & tampilan
```

**Catatan teknis di balik poin-poin itu** (referensi internal, bukan untuk
user):
- Hisab: rombak formula Ephemeris Waktu Sholat sesuai prosedur klasik
  (`159689d`); Awal Bulan pindah dari data dummy ke Astronomy Engine asli
  (`ba42cc8`).
- Fitur baru: Gerhana (`6112890`), Profil + upgrade auth ke Sanctum token
  (`3189f22`), arah Matahari/bayangan di Kiblat (`c8f6710`).
- Redesain: Splash/Login/Home, Waktu Sholat/Hijriyah/Gerhana, Kalender/Hari
  Besar, Konfigurasi/Profil — semua ke sistem warna navy & gold.
- Bug fix: tombol Hitung Ulang menutupi hasil (Bulan Hijriyah), konsistensi
  top bar/window insets di semua layar.

## 6. Known gotcha (buat pengalaman kali ini)

- Menu "App signing" di Play Console **sering pindah tempat** antar update
  UI mereka — per 2026-09-04 lokasinya: app → **Protected with Play** →
  klik baris **"Protect app signing key"**. Menu "App integrity" di sidebar
  "Test and release" sekarang cuma redirect ke situ juga. Kalau nanti pindah
  lagi, coba fitur search internal Play Console atau Help search dengan kata
  kunci "app signing key".
- Kalau lupa proses "salin file .template": maksudnya bikin file baru
  bernama `keystore.properties` (tanpa `.template`) di folder yang sama,
  isinya 4 baris config — bukan rename/edit file `.template`-nya langsung.
