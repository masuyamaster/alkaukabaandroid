# Notifikasi Adzan

### 1. Ringkasan (Overview)
- **Nama fitur**: Notifikasi Adzan + Personalisasi Suara
- **Deskripsi singkat**: Mengirim notifikasi otomatis (dengan opsi suara) tepat
  saat masuk waktu Subuh/Dzuhur/Ashar/Maghrib/Isya, tanpa perlu app dibuka.
  User bisa memilih apakah notifikasinya berupa adzan penuh, beep pelan (mis.
  untuk situasi di kantor), atau senyap (visual saja). Sebelum fitur ini
  dibangun (2026-09-05), app **tidak punya mekanisme notifikasi apa pun** —
  ini fondasi pertamanya, bukan sekadar penambahan opsi ke sistem yang sudah
  ada.

### 2. Entry point & prasyarat
- **Trigger notifikasi**: bukan dari UI, tapi dari `AlarmManager` yang
  dijadwalkan `AdzanScheduler` — dipicu ulang tiap hari oleh `AdzanRefreshWorker`
  (WorkManager periodic, jam 00:05) dan sekali lagi tiap app baru dibuka
  (`AlKaukabaApplication.onCreate()`).
- **Setting user**: row "Suara Notifikasi Adzan" di `KonfigurasiActivity`
  (`app/src/main/java/Site/elahady/alkaukaba/ui/konfigurasi/KonfigurasiActivity.kt`,
  fungsi `showAdzanSoundSheet()`) — lihat juga
  [konfigurasi.md](konfigurasi.md) untuk pola BottomSheetDialog yang dipakai
  ulang di sini.
- **Prasyarat runtime**:
  - `POST_NOTIFICATIONS` (Android 13+) — diminta lewat
    `ensureNotificationPrerequisites()` saat user membuka section ini.
  - Izin "Alarm & pengingat" / `SCHEDULE_EXACT_ALARM` (Android 12+) — dicek via
    `AlarmManager.canScheduleExactAlarms()`, kalau belum diarahkan ke
    `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Kalau user menolak,
    `AdzanScheduler` fallback ke `setAndAllowWhileIdle` (tidak-exact — notifikasi
    tetap muncul tapi bisa mundur beberapa menit).
  - Koneksi internet saat `AdzanRefreshWorker` jalan (jadwal sholat diambil dari
    Aladhan API lewat `PrayerRepository`, tidak ada cache lokal — lihat section
    7 untuk risikonya).

### 3. Titik masuk logika & navigasi
- `AdzanScheduler.scheduleFromTimings(context, timings: TimingPrayers)` — titik
  masuk utama kalau developer lain mau memicu ulang penjadwalan alarm secara
  manual (mis. setelah user ganti lokasi di Konfigurasi).
- `NotificationHelper.createChannels(context)` — daftar `NotificationChannel`
  yang ada (`adzan_playback`, `adzan_beep`, `adzan_silent`); tambah channel baru
  di sini kalau suatu saat ada mode suara baru.
- Tidak ada navigasi antar-Activity di fitur ini — semuanya background
  (Receiver/Service/Worker) sampai user tap notifikasi, yang membuka
  `MainActivity` (lihat `contentIntent`/`buildNotification` di
  `NotificationHelper.kt` dan `AdzanPlaybackService.kt`).

### 4. Struktur & alur data
Semua file baru di `app/src/main/java/Site/elahady/alkaukaba/notifikasi/`
kecuali `AlKaukabaApplication.kt` (root package):

| File | Peran |
|---|---|
| `AlKaukabaApplication.kt` | Application class custom — init channel + jadwalkan WorkManager (immediate + periodic 00:05) |
| `AdzanRefreshWorker.kt` | `CoroutineWorker` — fetch jadwal hari ini via `PrayerRepository`, resolve lokasi (manual/GPS/fallback Jakarta), lalu panggil `AdzanScheduler` |
| `AdzanScheduler.kt` | Pasang `AlarmManager.setExactAndAllowWhileIdle` per waktu sholat, `PendingIntent` ke `AdzanAlarmReceiver` |
| `AdzanAlarmReceiver.kt` | Diterima tepat saat alarm bunyi — baca `SessionManager.getAdzanSoundMode()` lalu branch ke Service/NotificationHelper |
| `AdzanPlaybackService.kt` | Foreground service (`mediaPlayback`) — `MediaPlayer` play `res/raw/adzan_marrakesh.mp3` untuk mode Adzan Penuh, ada tombol Stop di notifikasi |
| `NotificationHelper.kt` | Definisi `NotificationChannel` + post notifikasi untuk mode Beep/Senyap |
| `BootReceiver.kt` | `BOOT_COMPLETED`/`MY_PACKAGE_REPLACED` — jadwalkan ulang alarm (hilang saat reboot) |

Alur data: `AlKaukabaApplication` (jadwal awal) atau `BootReceiver` (reboot) →
`WorkManager` → `AdzanRefreshWorker` → `PrayerRepository` (Aladhan API,
**reuse langsung**, tidak ada layer baru) → `AdzanScheduler` → `AlarmManager` →
`AdzanAlarmReceiver` → `AdzanPlaybackService` / `NotificationHelper`.

Setting user: `KonfigurasiActivity` ↔ `SessionManager` (key
`ADZAN_SOUND_MODE`, sama seperti key lain di kelas itu — SharedPreferences
biasa, bukan DataStore).

### 5. Dependencies & tech stack khusus
- `androidx.work:work-runtime-ktx:2.8.1` (baru ditambahkan). **Bukan 2.9.0**:
  versi itu mensyaratkan `compileSdk 34+`, sedangkan project ini masih
  `compileSdk 33` — jangan naikkan versi WorkManager tanpa menaikkan
  `compileSdk` (dan cek dampak AGP 7.2.2 yang dipakai project ini, lihat
  catatan di `app/build.gradle`).
- Tidak ada library alarm/notifikasi tambahan lain — pakai `AlarmManager`,
  `NotificationCompat`, dan `MediaPlayer` bawaan Android.

### 6. Testing
- **Belum ada test otomatis** untuk fitur ini (gap, bukan sengaja dilewati).
- Verifikasi manual yang sudah dilakukan: `gradlew compileDebugKotlin` dan
  `gradlew assembleDebug` — BUILD SUCCESSFUL, APK debug ~13MB.
- **Belum dilakukan** (perlu sebelum rilis): test di device fisik dengan waktu
  sholat sungguhan atau lewat broadcast manual:
  ```
  adb shell am broadcast \
    --es prayer_name "Subuh" \
    -n site.elahady.alkaukaba/.notifikasi.AdzanAlarmReceiver
  ```
  (catatan: intent yang dikirim `AdzanScheduler` tidak diberi `action` — hanya
  ditarget lewat component name + extra `prayer_name`, jadi broadcast manual di
  atas tidak perlu `-a`)
  — ganti pilihan suara di Konfigurasi lalu ulangi, pastikan mode yang aktif
  yang kepakai (bukan yang di-cache saat scheduling). Test juga reboot
  (`adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -n
  site.elahady.alkaukaba/.notifikasi.BootReceiver` atau reboot device
  sungguhan) untuk pastikan `BootReceiver` jalan.

### 7. Known issues & TODOs
- Hanya **1 pilihan "Adzan Penuh"** (rekaman CC0 "EveningCallToPrayer
  Marrakesh 5.1" oleh blaukreuz, freesound.org/people/blaukreuz/sounds/520233,
  durasi 3:36) — bukan multi-muadzin seperti rencana awal di Notion. Nama qari
  terkenal (Mishary Alafasy dll.) yang beredar di GitHub tidak punya lisensi
  jelas, jadi sengaja tidak dipakai. Kalau mau tambah pilihan lain, cari
  rekaman CC0 terverifikasi (cek langsung halaman lisensinya di Freesound,
  jangan percaya hasil pencarian saja) sebelum dibundel ke `res/raw`.
- **Battery optimization OEM** (Xiaomi/Oppo/Vivo dkk.) belum ditangani — alarm
  exact bisa saja tetap di-kill di background pada device tertentu meski app
  sudah pakai `setExactAndAllowWhileIdle`. Perlu diarahkan ke pengaturan
  whitelist battery optimizer per-OEM kalau ada laporan notifikasi tidak
  konsisten.
- **Tidak ada fallback jadwal offline** — kalau `AdzanRefreshWorker` gagal fetch
  (tidak ada internet saat itu), `Result.retry()` dipanggil tapi tidak ada
  jadwal cadangan dari hari sebelumnya. WorkManager akan retry dengan backoff
  default, tapi kalau tetap gagal sampai lewat tengah malam, hari itu tidak
  ada alarm sama sekali.
- Belum ada UI untuk menonaktifkan notifikasi per-waktu-sholat (mis. matikan
  cuma untuk Dzuhur) — saat ini semua-atau-tidak-sama-sekali per mode suara.
- Belum di-commit ke git per 2026-09-05 (lihat status di Notion "🚀
  Pengembangan Al-Kaukaba" → entry "Personalisasi Notifikasi Adzan").
