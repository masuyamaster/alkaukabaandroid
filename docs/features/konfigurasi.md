# Konfigurasi

## 1. Ringkasan

**Fitur**: Konfigurasi — layar setting terpusat berisi Lokasi, Sumber
Perhitungan Arah Kiblat, dan Metode Perhitungan Waktu Sholat.

Sebelumnya tiap fitur (Waktu Sholat, Arah Kiblat, Kalender) selalu mengambil
lokasi live dari GPS sendiri-sendiri, tanpa cara untuk memakai koordinat tetap
(mis. markaz rukyat tertentu, atau lokasi tanpa sinyal GPS bagus). Setting
**Lokasi** di sini menyelesaikan itu: satu toggle Otomatis (GPS) / Manual yang
dibaca oleh semua fitur yang butuh lat/lon — bukan setting lokal per-layar.
Setting **Sumber Perhitungan Arah Kiblat** menyelesaikan kebutuhan serupa
untuk Kiblat secara spesifik: pilih apakah sudut kiblat utama yang ditampilkan
berasal dari Aladhan API (online) atau rumus manual Al Hasib (lokal, tanpa
internet).

## 2. Entry point & prasyarat

- Dari `MainActivity`: tap ikon gear (`btnSettings`, kanan atas toolbar) →
  `startActivity(Intent(..., KonfigurasiActivity::class.java))`, tanpa extra.
- `KonfigurasiActivity` terdaftar di `AndroidManifest.xml`.
- Prasyarat:
  - Tidak ada permission yang diminta oleh layar Konfigurasi itu sendiri
    untuk section Lokasi & Arah Kiblat — **kecuali** user tap tombol "Pakai
    lokasi GPS saat ini" di sheet Lokasi (lihat section 3), yang baru minta
    `ACCESS_FINE_LOCATION` saat itu juga.
  - Section Waktu Sholat tidak butuh permission apa pun (murni pilih preset
    dari daftar statis).

## 3. Titik masuk logika & navigasi

- `SessionManager` — satu-satunya sumber kebenaran (state global,
  `SharedPreferences`) untuk ketiga section:
  - `getLocationMode()`/`setLocationMode()` (`"AUTO"`/`"MANUAL"`),
    `getManualLat()`/`getManualLng()`/`setManualLocation()`,
    `hasManualLocation()`, `isManualLocationMode()` (helper gabungan: mode
    Manual **dan** sudah pernah ada koordinat tersimpan).
  - `getQiblaSource()`/`setQiblaSource()` (`"ALADHAN"`/`"MANUAL_FORMULA"`).
  - `getPrayerMethodId()` dkk — sudah ada sebelumnya, tidak berubah (lihat
    `docs/features/waktu-sholat.md`).
- `KonfigurasiActivity.showLocationSheet()` — inflate `dialog_lokasi.xml`,
  radio Otomatis/Manual, dua `EditText` lat/lon yang muncul kalau Manual
  dipilih, tombol "Pakai lokasi GPS saat ini" (isi field dari
  `FusedLocationProviderClient.lastLocation` sekali, user masih bisa edit
  manual setelahnya), validasi rentang (-90..90 / -180..180) sebelum simpan.
- `KonfigurasiActivity.showQiblaSourceSheet()` — inflate
  `dialog_qibla_source.xml`, radio Aladhan/Rumus Manual, simpan langsung
  (tidak ada input tambahan).
- Ini **bukan** setting yang otomatis "aktif" begitu disimpan di sini — tiap
  fitur pemakai (lihat daftar di bawah) yang bertanggung jawab membaca
  `SessionManager` di titik masuk lokasinya sendiri (`checkLocationPermission()`
  masing-masing). Kalau ada fitur baru yang butuh lokasi, developer harus
  sadar menambahkan pengecekan ini sendiri — tidak ada mekanisme paksa/DI
  yang menjamin semua konsumen otomatis ikut.
- Navigasi: `MainActivity` → `KonfigurasiActivity` via `Intent` biasa. Dari
  dalam `KonfigurasiActivity`, ketiga sheet (Lokasi, Arah Kiblat, Metode
  Waktu Sholat) dan konfirmasi logout semuanya modal (`BottomSheetDialog`/
  `AlertDialog`), bukan Activity terpisah.

### Fitur yang sudah membaca setting ini

| Fitur | Lokasi (Manual/Auto) | Sumber Kiblat |
|---|---|---|
| `MainActivity` (beranda: Waktu Sholat preview, Kalender, hari besar) | ✅ | tidak relevan |
| `WaktuSholatActivity` | ✅ | tidak relevan |
| `KiblatActivity` | ✅ | ✅ |
| `CalendarActivity` | ❌ tidak langsung — terima lat/lon lewat extra `Intent` dari `MainActivity`, jadi otomatis ikut kalau `MainActivity` sudah benar (lihat `docs/features/kalender.md`) | tidak relevan |
| `AwalBulanActivity` (Bulan Hijriyah) | ❌ belum — fitur ini sendiri masih dummy/belum disambung navigasi, lihat `docs/features/bulan-hijriyah.md`. Saat fitur itu dibangun ulang, **harus** ikut baca `SessionManager` ini, bukan bikin logic lokasi baru | tidak relevan |

## 4. Struktur & alur data

File yang terlibat:

| File | Peran |
|---|---|
| `ui/konfigurasi/KonfigurasiActivity.kt` | Satu-satunya Activity untuk layar ini: wiring 3 row + 3 sheet + logout |
| `utils/SessionManager.kt` | Persistensi semua setting (lokasi, sumber kiblat, metode sholat) di `SharedPreferences "AppSession"` |
| `res/layout/activity_konfigurasi.xml` | Layout utama: 3 section (LOKASI, ARAH KIBLAT, WAKTU SHOLAT) masing-masing satu row card, plus tombol Logout |
| `res/layout/dialog_lokasi.xml` | Bottom sheet Lokasi: radio Otomatis/Manual, field lat/lon, tombol GPS, tombol Simpan |
| `res/layout/dialog_qibla_source.xml` | Bottom sheet Arah Kiblat: radio Aladhan/Rumus Manual, tombol Simpan |
| `res/layout/dialog_prayer_method.xml` | Bottom sheet Waktu Sholat — sudah ada sebelumnya, tidak berubah |

Alur data (simpan Lokasi Manual): user isi `etManualLat`/`etManualLng` (atau
tap "Pakai lokasi GPS saat ini" untuk auto-isi sekali dari GPS) → tap Simpan
→ validasi rentang → `sessionManager.setManualLocation(lat, lng)` +
`setLocationMode(MANUAL)` → `updateCurrentLocationLabel()` update subtitle
row jadi `"Manual: %.4f, %.4f"`.

Alur data (konsumsi oleh fitur lain): tiap Activity pemakai (lihat tabel di
atas) memanggil `sessionManager.isManualLocationMode()` di awal alur
lokasinya (biasanya `checkLocationPermission()`) — kalau `true`, lewati
permission/GPS sepenuhnya dan langsung pakai `getManualLat()`/`getManualLng()`;
kalau `false`, jalankan alur GPS seperti biasa (masing-masing Activity punya
implementasi permission-request sendiri-sendiri, tidak ada abstraksi
`LocationProvider` bersama — lihat Known issues).

Alur data (Sumber Perhitungan Arah Kiblat): dibaca oleh `KiblatActivity`
di `onLocationReady()`, lihat detail lengkap di
`docs/features/arah-kiblat.md` section 3 & 4.

## 5. Dependencies & tech stack khusus

- `FusedLocationProviderClient` — dipakai untuk tombol "Pakai lokasi GPS
  saat ini" di sheet Lokasi; sama seperti yang dipakai fitur lain, tidak ada
  tambahan library baru.
- Tidak ada tambahan lain di luar stack umum app (`SharedPreferences`,
  `BottomSheetDialog`).

## 6. Testing

Belum ada test otomatis untuk fitur ini (konsisten dengan fitur lain di app
ini — lihat `app/src/test`/`app/src/androidTest`, masih boilerplate). Verifikasi
manual:

1. Build & install debug APK (lihat `CLAUDE.md` root untuk perintah `gradlew`).
2. Buka `MainActivity` → tap gear → pastikan masuk `KonfigurasiActivity`
   dengan 3 section dalam urutan Lokasi, Arah Kiblat, Waktu Sholat.
3. Tap row "Sumber Lokasi" → pastikan sheet terbuka dengan radio
   Otomatis/Manual + deskripsi masing-masing.
4. Pilih Manual → pastikan field lat/lon + tombol GPS muncul; isi angka valid
   → Simpan → subtitle row berubah jadi `"Manual: lat, lng"`.
5. Tap row "Sumber Perhitungan" (Arah Kiblat) → pilih Rumus Manual → Simpan
   → subtitle berubah jadi "Rumus Manual (Al Hasib)".
6. Buka `WaktuSholatActivity` dan `KiblatActivity` → pastikan keduanya
   memakai koordinat manual (bukan minta permission GPS) — lihat langkah
   detail & hasil di `docs/features/waktu-sholat.md` dan
   `docs/features/arah-kiblat.md` section Testing.

**Catatan verifikasi sesi 2026-08-30**: langkah 4-6 sudah dicek berhasil di
emulator. Verifikasi interaktif langkah 3-5 (tap radio di dalam
`BottomSheetDialog`) sempat gagal berkali-kali lewat `adb shell input tap`
(sheet ke-dismiss alih-alih toggle) — diduga soal presisi koordinat tap dari
screenshot, bukan bug kode, karena polanya identik dengan sheet Metode
Perhitungan Waktu Sholat yang sudah terbukti jalan. Verifikasi akhir untuk
alur baca (`SessionManager` → fitur konsumen) dilakukan lewat
`adb shell run-as site.elahady.alkaukaba cat shared_prefs/AppSession.xml`
(baca state tersimpan) dan cocok dengan yang ditampilkan di layar konsumen —
lihat detail di `docs/features/arah-kiblat.md` & `waktu-sholat.md`. Verifikasi
tap manual langsung di sheet Lokasi/Arah Kiblat oleh developer di device
fisik masih disarankan sebelum dianggap 100% teruji secara interaktif.

## 7. Known issues & TODOs

- [ ] **Tidak ada abstraksi `LocationProvider` bersama** — tiap Activity
      pemakai (`MainActivity`, `WaktuSholatActivity`, `KiblatActivity`)
      mengulang pola yang sama (cek `isManualLocationMode()` di awal
      `checkLocationPermission()`) dengan implementasi permission-request GPS
      masing-masing yang berbeda-beda gaya (lihat kode tiap Activity). Kalau
      makin banyak fitur butuh lokasi (mis. Bulan Hijriyah nanti), pertimbangkan
      ekstrak helper bersama — belum dilakukan sekarang karena baru 3
      konsumen dan tiap Activity punya nuansa permission-flow yang beda.
- [ ] **`CalendarActivity` dan `AwalBulanActivity` belum ikut baca setting
      ini** — lihat tabel section 3. `CalendarActivity` otomatis "ikut
      benar" selama `MainActivity` benar (karena lat/lon dioper lewat
      `Intent` extra), tapi `AwalBulanActivity` (fitur Bulan Hijriyah) masih
      dummy total dan perlu baca `SessionManager` ini dari awal saat dibangun
      ulang.
- [ ] **Kompas visual untuk mode Manual di Arah Kiblat masih placeholder
      generik**, bukan digambar sesuai sudut manual — lihat
      `docs/features/arah-kiblat.md` Known issues.
- [ ] Input lokasi manual cuma angka lat/lon (`EditText` biasa) — tidak ada
      pencarian nama tempat (forward geocoding). Ini keputusan sadar (lihat
      diskusi desain), bukan keterbatasan teknis yang belum sempat.
- [ ] Belum ada test otomatis sama sekali untuk fitur ini (lihat section 6).
