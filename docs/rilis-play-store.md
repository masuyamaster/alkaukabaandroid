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
  §2).
- **Update 2026-09-08: request upload key reset SUDAH APPROVED.** Dikonfirmasi
  dengan cara paling pasti — sertifikat yang tampil di Play Console → app →
  Protected with Play → "Protect app signing key" → "App signing" →
  "Upload key certificate" sekarang **cocok persis** (SHA1 & SHA256) dengan
  `upload_certificate.pem` yang diserahkan di §2. Tidak perlu tunggu lagi,
  sudah bisa upload AAB dengan key baru.

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
- `versionCode` sudah dinaikkan **6 → 7 → 8** (versionCode 7 sempat
  ke-"bakar" karena sudah pernah diupload sekali ke draft release Play
  Console sebelum fix §3a ditambahkan — Play Console menolak upload ulang
  dengan versionCode yang sama meski isinya beda, errornya "Version code 7
  has already been used"). `versionName` tetap **"1.7"** (cuma versionCode
  internal yang naik, bukan rilis fitur baru).
- Build command (path JDK/SDK contoh salah satu mesin dev, sesuaikan):
  ```
  JAVA_HOME="C:/Program Files/Java/jdk-17.0.2" ANDROID_HOME="C:/Users/<user>/AppData/Local/Android/Sdk" ./gradlew.bat bundleRelease --console=plain
  ```
- Output AAB signed (versionCode 8): `app/build/outputs/bundle/release/
  app-release.aab` — sudah di-build & diverifikasi sertifikatnya cocok
  dengan `keystore-upload-2026.jks` DAN cocok dengan upload key certificate
  yang tampil di Play Console (SHA1 `66:E9:E6:BA:B6:7D:1D:99:EA:A9:CF:14:
  CF:53:4B:48:8C:73:CD:1B`). **Siap upload**, tinggal ganti file lama di
  draft release Play Console (yang masih versionCode 7, sudah error) dengan
  file ini.

### 3a. Fix tambahan yang masuk sebelum upload (commit di repo ini)

Ditemukan & diperbaiki selagi proses prepare-release di Play Console
menampilkan warning, semuanya sudah di-commit ke `main`:

- `b34e432` — deklarasikan `<uses-feature android:name="android.hardware.
  camera" android:required="false">` (+ `camera.autofocus`) di
  `AndroidManifest.xml`. Tanpa ini, permission CAMERA (dipakai cuma untuk
  foto profil opsional di `ProfileActivity`, ada alternatif pilih galeri)
  otomatis membuat Android mewajibkan hardware kamera untuk listing Play
  Store — ini penyebab warning "461 device tidak lagi didukung" saat
  prepare release. Setelah fix, warning itu hilang total.
- `e8c9d79` — nama "Al-Hasib" (penyusun) di Laporan Hasil Hisab
  (`LaporanHisabActivity.kt`) sengaja di-hardcode jadi "Roziq Rizal",
  bukan lagi dinamis dari sesi login — karena hisab di app ini memang
  selalu disusun oleh satu orang yang sama, terlepas siapa yang login.
- `59e3d56` — bump versionCode 7 → 8 (lihat alasan di atas).

Warning lain yang **boleh diabaikan** (tidak blocking, sudah dicek):
permission baru (CAMERA/WRITE_EXTERNAL_STORAGE) perlu di-accept ulang user
lama, ukuran APK naik signifikan, tidak ada file deobfuscation/mapping
(R8/Proguard) — lihat TODO §7 buat yang terakhir ini.

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

## 7. Blocker baru: Data safety declaration (ditemukan 2026-09-08)

Setelah AAB versionCode 8 siap upload, ternyata **Publishing overview**
Play Console kasih blocker terpisah yang harus dibereskan dulu sebelum bisa
"send for review": **"Incomplete data safety declaration"**. Lokasinya:
Play Console → app → **Policy and programs → App content → Data safety**.
Statusnya baru dikerjakan sebagian, **belum selesai** — lanjutkan dari sini.

### Hasil audit kode (2026-09-08) — jadi acuan jawaban form

Sudah ditelusuri dari source app (`alkaukabaandroid`) + coba cek backend,
supaya jawaban Data Safety akurat (bukan asal tebak):

| Data | Dikumpulkan? | Dikirim ke mana? | Untuk apa? |
|---|---|---|---|
| Lokasi (approx/precise) | Ya | **Pihak ketiga**: `api.aladhan.com` (bukan ke `alkaukaba.com` sendiri!) | Hitung waktu sholat/kiblat/kalender Hijriyah (`PrayersApiService.kt`) |
| Nama (username) | Ya | Server sendiri: `alkaukaba.com` | Akun (`AuthModels.kt`) |
| Email | Ya | Server sendiri: `alkaukaba.com` (+ Google SDK utk login Google) | Akun/login |
| Password | Ya | Server sendiri: `alkaukaba.com` | Autentikasi |
| Foto profil | Ya | Server sendiri: `alkaukaba.com` (`upload_avatar`) | Kustomisasi akun (`ProfileActivity.kt`) |
| Device/phone ID | **Tidak** | - | `READ_PHONE_STATE` di-declare di manifest tapi **tidak dipakai sama sekali** di kode manapun — kandidat kuat buat dihapus izinnya (lihat TODO) |
| Analytics/crash SDK | **Tidak ada** | - | Tidak ada Firebase/Crashlytics/dsb di `build.gradle` |

Metode pembuatan akun yang harus dicentang di form: **"Username and
password"** (register/login email+password) + **"OAuth"** (Google Sign-In).

### BLOCKER: Delete account URL — SUDAH DIBUAT (2026-09-09)

Form Data Safety minta **"Delete account URL"** — link publik (bukan
in-app) yang menjelaskan cara user minta akun+data dihapus. App ini
**sudah punya fitur hapus akun in-app** (`ProfileActivity.kt`, konfirmasi
password → `delete_account` endpoint).

**Kejanggalan repo backend sudah diklarifikasi**: repo yang benar-benar
live & dipanggil app adalah **`alkaukabaweb`** (`C:\xampp\htdocs\
alkaukabaweb`) — dikonfirmasi lewat `AuthController.php` yang memang
punya endpoint `register`/`login`/`google_login`/`update_profile`/
`change_password`/`delete_account` via `POST /api.php?action=...`, cocok
dengan komentar di `SessionManager.kt`. `alkaukabawebserver` bukan repo
yang dipakai (skeleton kosong, diabaikan).

Halaman publik sudah dibuat: `resources/views/hapus-akun.blade.php` +
route `GET /hapus-akun` (name `hapus-akun`) di `routes/web.php`, di-commit
di `alkaukabaweb` (`7a1c1cb`). Isinya instruksi hapus akun lewat app
(Profil → Hapus Akun) + tabel data yang dihapus. **Belum di-deploy ke
`alkaukaba.com`** — perlu push ke `main` lalu `git pull` manual di VPS
(lihat workflow deploy di `alkaukabaweb/CLAUDE.md`) supaya URL
`https://alkaukaba.com/hapus-akun` bisa dipakai di form Data Safety.

### TODO lanjutan

1. ~~Buat halaman "Hapus Akun" publik~~ — **selesai**, tinggal deploy
   (push `alkaukabaweb` ke `main` + `git pull` di VPS).
2. Isi form Data Safety pakai tabel di §7 + URL
   `https://alkaukaba.com/hapus-akun`, lalu submit.
3. Baru setelah Data Safety selesai, kembali ke draft release production →
   upload AAB versionCode 8 → lanjut ke tahap review/rollout (lihat §4).
4. (Optional, tidak blocking) Pertimbangkan hapus `<uses-permission
   android:name="android.permission.READ_PHONE_STATE" />` dari manifest
   karena tidak dipakai — biar jawaban Data Safety soal "Device or other
   IDs" bisa jujur "not collected" tanpa perlu penjelasan tambahan.
5. (Optional, tidak blocking) Setup upload mapping/deobfuscation file
   (R8/Proguard) ke Play Console — warning muncul saat prepare release,
   diabaikan dulu, tidak menghalangi rilis.

### Draft jawaban Data Safety form (siap isi, 2026-09-09)

Mapping tabel audit di atas ke kategori form Play Console. **Nama kategori
persis bisa berubah sedikit tergantung versi UI Play Console** — cocokkan
istilahnya, tapi isi/jawabannya tetap ini.

**Halaman pembuka "Data collection and security":**
- "Does your app collect or share any of the required user data types?" →
  **Yes**
- "Is all of the user data collected by your app encrypted in transit?" →
  **Yes** (HTTPS/SSL via Certbot di `alkaukaba.com` & `api.alkaukaba.com`,
  lihat `alkaukabaweb/CLAUDE.md`)
- "Do you provide a way for users to request that their data is
  deleted?" → **Yes**, isi **Delete account URL**:
  `https://alkaukaba.com/hapus-akun`

**Data types:**

| Kategori Play | Sub-tipe | Dikumpulkan | Dibagikan ke pihak ketiga | Tujuan |
|---|---|---|---|---|
| Location | Approximate location **dan** Precise location | Yes | Yes — `api.aladhan.com` | App functionality |
| Personal info | Name | Yes | No | Account management |
| Personal info | Email address | Yes | No* | Account management |
| Personal info | Other info (kredensial akun / password) | Yes | No | Account management |
| Photos and videos | Photos | Yes | No | Account management (foto profil, opsional) |
| Device or other IDs | — | **No** | — | (lihat item 4 TODO — `READ_PHONE_STATE` di-declare tapi tidak dipakai) |

\* Email juga lewat Google Sign-In SDK saat login Google — itu pertukaran
data user dengan Google secara langsung (user login ke akun Google-nya
sendiri), bukan app yang membagikan data ke Google sebagai pihak ketiga,
jadi tetap **No** untuk "shared". Kalau versi form terbaru minta declare
SDK pihak ketiga secara terpisah, cukup sebut Google Sign-In sebagai SDK
autentikasi.

Untuk **Location**, cek "Is this data processed ephemerally?" → jawaban
**Yes** kalau memang lokasi cuma diteruskan ke `PrayersApiService.kt` →
`api.aladhan.com` saat itu juga dan **tidak pernah disimpan** ke database
sendiri (`laravel_api`) — **konfirmasi ini dulu ke kode sebelum submit**,
karena kalau ternyata ada logging/cache lokasi di server, jawabannya
harus **No**.

Untuk setiap sub-tipe di atas, form biasanya juga minta jawaban "Is this
data required or optional?" → semuanya **Required** kecuali foto profil
(**Optional**, karena `ProfileActivity.kt` punya alternatif tanpa upload
foto).

**Metode pembuatan akun** (kalau form menanyakan ini secara terpisah):
centang **"Username and password"** + **"OAuth"** (Google Sign-In), sesuai
catatan §7 di atas.
