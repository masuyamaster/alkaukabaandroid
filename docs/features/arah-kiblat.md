# Arah Kiblat

## 1. Ringkasan

**Fitur**: Arah Kiblat — kompas visual yang menunjukkan arah kiblat dari lokasi
user saat ini.

User membuka layar ini untuk melihat arah kiblat lewat kompas yang berputar
mengikuti orientasi fisik HP (sensor rotasi), dibandingkan dengan sudut kiblat
hasil hitung berdasarkan lokasi GPS. Saat heading HP sejajar dengan sudut
kiblat (dalam toleransi tertentu), tampilan kompas berubah warna sebagai
penanda "sudah pas".

## 2. Entry point & prasyarat

- Dari `MainActivity`: tap tombol **`btKiblat`** → `startActivity(Intent(...,
  KiblatActivity::class.java))` (`MainActivity.kt` baris ~200-202). Tidak ada
  extra yang dibawa lewat `Intent`.
- `KiblatActivity` terdaftar di `AndroidManifest.xml`
  (`site.elahady.alkaukaba.ui.arahkiblat.KiblatActivity`, `exported="false"`).
- Prasyarat:
  - Permission runtime `ACCESS_FINE_LOCATION` + `ACCESS_COARSE_LOCATION`
    (diminta lewat `ActivityResultContracts.RequestMultiplePermissions()` saat
    `onCreate`). Kalau ditolak, cuma tampil `Toast` peringatan — layar tetap
    terbuka tanpa data lokasi.
  - GPS provider harus aktif — `checkGpsEnabled()` cek
    `LocationManager.GPS_PROVIDER`; kalau mati, user diarahkan ke
    `Settings.ACTION_LOCATION_SOURCE_SETTINGS` (tidak otomatis lanjut, user
    harus balik manual ke app).
  - Sensor rotasi perangkat: `Sensor.TYPE_ROTATION_VECTOR` (kompas HP). Tidak
    ada permission Android untuk ini (bukan *dangerous permission*), tapi
    kalau device tidak punya sensor tersebut `rotationSensor` bernilai `null`
    dan kompas visual sekadar diam — tidak ada pesan error ke user untuk
    kasus ini (lihat Known issues).
  - `INTERNET` (dideklarasikan app-wide di manifest) — dipakai untuk request
    sudut kiblat ke Aladhan API dan load gambar kompas via Glide.

## 3. Titik masuk logika & navigasi

- `KiblatViewModel.fetchQiblaAngle(lat, lon)` — satu-satunya method public,
  dipanggil dari `KiblatActivity.onLocationReady()` begitu lokasi didapat.
  Hasilnya di-expose lewat `LiveData<Double> qiblaAngle` (derajat, dari API)
  dan `LiveData<String> error`.
- `KiblatRepository.getQiblaAngle(lat, lon)` — jembatan ke
  `AladhanApi.getQiblaDirection(lat, lon)` (endpoint Aladhan
  `GET v1/qibla/{latitude}/{longitude}`), balikin `.data.direction`.
- `QiblaCalculator.calculateQibla(userLat, userLong)` — util murni (object,
  bukan class), hitung sudut kiblat sendiri secara lokal pakai trigonometri
  bola (tanpa panggil API). **Bukan dipakai oleh `KiblatActivity`/
  `KiblatViewModel`** — satu-satunya pemakainya saat ini adalah
  `PrayerTimesViewModel.calculateQibla()` (lihat `docs/features/waktu-sholat.md`
  tidak menyebutnya, tapi kodenya ada di
  `viewmodel/waktusholat/PrayerTimesViewModel.kt` baris 68-72) untuk
  menyusun teks "Detail Rumus Kiblat" (`qiblaDetailText`) yang ditampilkan di
  layar Waktu Sholat, bukan di layar Arah Kiblat ini. Lihat bagian "Kenapa"
  di bawah untuk konsekuensinya.
- Navigasi: `MainActivity` → `KiblatActivity` via `Intent` biasa, satu arah,
  tanpa extra. Tombol back di toolbar (`binding.toolbar
  .setNavigationOnClickListener`) memanggil `onBackPressedDispatcher
  .onBackPressed()`. Tidak ada navigasi lanjutan ke Activity lain di dalam
  fitur ini.
- `ArahKiblatActivity` (di file terpisah, folder yang sama) berisi struktur
  navigasi lain — `ViewPager` dua tab (`KiblatFragment` "Kiblat" dan
  `FalakiyahFragment` "Detail Perhitungan") lewat
  `FragmentStatePagerAdapter`. **Activity ini tidak terdaftar di
  `AndroidManifest.xml` dan tidak dipicu dari mana pun di codebase** (sudah
  digrep, hasil nol selain deklarasi class-nya sendiri) — lihat Known issues.

## 4. Struktur & alur data

File yang terlibat:

| File | Peran |
|---|---|
| `ui/arahkiblat/KiblatActivity.kt` | Activity aktif satu-satunya untuk fitur ini (terdaftar di manifest, dipicu dari `MainActivity`). Urus permission lokasi, cek GPS aktif, ambil lokasi via `FusedLocationProviderClient`, dengarkan sensor rotasi, render gambar kompas via Glide, hitung alignment heading vs sudut kiblat |
| `ui/arahkiblat/ArahKiblatActivity.kt` | Activity alternatif ber-tab (`ViewPager`) — **tidak terdaftar di manifest, dead code**, lihat Known issues |
| `ui/arahkiblat/KiblatFragment.kt` + `res/layout/fragment_kiblat.xml` | Tab pertama `ArahKiblatActivity` — masih boilerplate "blank fragment" bawaan Android Studio (`TODO: Update blank fragment layout`, teks `hello_blank_fragment`), tidak ada implementasi |
| `ui/arahkiblat/FalakiyahFragment.kt` + `res/layout/fragment_falakiyah.xml` | Tab kedua `ArahKiblatActivity` ("Detail Perhitungan") — sama, masih blank fragment boilerplate, tidak ada implementasi |
| `viewmodel/arahkiblat/KiblatViewModel.kt` | `fetchQiblaAngle(lat, lon)` di `viewModelScope`, expose `qiblaAngle`/`error` sebagai `LiveData` |
| `viewmodel/arahkiblat/KiblatViewModelFactory.kt` | DI manual (tanpa framework) — construct `KiblatRepository(RetrofitClient.instance)` lalu `KiblatViewModel` |
| `repo/arahkiblat/KiblatRepository.kt` | `getQiblaAngle()` — satu-satunya pemanggil `AladhanApi.getQiblaDirection` |
| `api/PrayersApiService.kt` (`AladhanApi` interface + `RetrofitClient`, base URL `https://api.aladhan.com/`) | Retrofit service bersama, dipakai juga oleh fitur Waktu Sholat (lihat `docs/features/waktu-sholat.md`) |
| `utils/QiblaCalculator.kt` | Util murni, hitung sudut kiblat lokal dengan trigonometri bola. Dipakai oleh `PrayerTimesViewModel`, **bukan** oleh fitur Arah Kiblat ini |
| `res/layout/activity_kiblat.xml` | Layout `KiblatActivity`: toolbar, `imgCompass`, `txtQiblaValue`, `txtLocation`, `qiblaAngleContainer`, `calibrationHint` |
| `res/layout/activity_arah_kiblat.xml` | Layout `ArahKiblatActivity` (tab layout + `ViewPager`) — hanya dipakai Activity yang dead code |

Alur data (sudut kiblat numerik, `txtQiblaValue`): `KiblatActivity.onCreate()`
→ `checkLocationPermission()` → `checkGpsEnabled()` →
`getLastLocation()`/`requestNewLocation()` (via `FusedLocationProviderClient`)
→ `onLocationReady(lat, lon)` → `viewModel.fetchQiblaAngle(lat, lon)` →
`KiblatViewModel` (coroutine di `viewModelScope`) → `KiblatRepository
.getQiblaAngle()` → `AladhanApi.getQiblaDirection(lat, lon)` →
`.data.direction` → `LiveData qiblaAngle` → observer di Activity set
`binding.txtQiblaValue.text` **dan** simpan ke variabel `qiblaAngle: Float`
yang dipakai untuk cek alignment kompas.

Alur data (gambar kompas visual, `imgCompass` background): dipanggil
langsung dari `onLocationReady()` lewat `loadQiblaCompass(lat, lon)` — **tidak
lewat ViewModel/Repository**, Activity langsung `Glide.load()` URL
`https://api.aladhan.com/v1/qibla/{lat}/{lon}/compass` (endpoint Aladhan yang
mengembalikan gambar kompas siap-pakai bertanda kiblat).

Alur data (heading/orientasi HP): `onResume()` daftarkan
`sensorListener` ke `Sensor.TYPE_ROTATION_VECTOR` (`SENSOR_DELAY_GAME`) →
`onSensorChanged()` hitung rotation matrix → azimuth mentah → dihaluskan
(`unwrapAngle()` + `lowPassFilter()`, faktor `0.15f`) jadi `currentAzimuth` →
`rotateCompassSmooth()` rotasikan `imgCompass` sebesar `-currentAzimuth` lalu
panggil `checkQiblaAlignment()` yang membandingkan `currentAzimuth` vs
`qiblaAngle` (dari API di alur pertama) dengan threshold `3°`
(`qiblaThresshold`) — kalau selaras, background & warna teks
`qiblaAngleContainer` berubah jadi versi "match" (`bg_qibla_match`, teks
putih), kalau tidak kembali ke default (`bg_qibla_angle`, teks hitam).
`onAccuracyChanged()` juga menampilkan/menyembunyikan `calibrationHint` saat
akurasi sensor rendah/unreliable.

Alur data (teks lokasi): `onLocationReady()` juga panggil
`getAddressFromLatLong(lat, lon)` — `Geocoder.getFromLocation()` dipanggil
**sinkron di main thread** (bukan di coroutine/background thread), hasilnya
langsung di-set ke `binding.txtLocation`.

## 5. Dependencies & tech stack khusus

- **Glide** — hanya dipakai di fitur ini (`KiblatActivity.loadQiblaCompass()`)
  untuk load gambar kompas dari URL Aladhan; tidak dipakai di fitur lain.
- **Google Play Services Location** (`FusedLocationProviderClient`,
  `LocationRequest`, `Priority.PRIORITY_HIGH_ACCURACY`) — ambil lokasi GPS
  presisi tinggi.
- `android.hardware.SensorManager` + `Sensor.TYPE_ROTATION_VECTOR` — bagian
  Android SDK bawaan (bukan third-party), dipakai untuk baca orientasi
  kompas HP.
- `android.location.Geocoder` — bagian Android SDK bawaan, reverse-geocode
  lat/lon jadi teks lokasi (kecamatan/kota/negara).
- Retrofit + Gson ke Aladhan API — sama seperti fitur Waktu Sholat, tidak
  ada tambahan khusus di luar yang sudah dipakai app-wide.

## 6. Testing

Belum ada test otomatis untuk fitur ini — sudah dicek `app/src/test` dan
`app/src/androidTest`, tidak ada file yang menyebut "Kiblat"/"Qibla" sama
sekali. Verifikasi saat ini manual:

1. Build & install debug APK (lihat `CLAUDE.md` root untuk perintah
   `gradlew`).
2. Buka `MainActivity` → tap tombol Arah Kiblat (`btKiblat`) → pastikan
   masuk ke `KiblatActivity` (bukan crash — kalau ternyata yang terpanggil
   `ArahKiblatActivity`, itu akan crash `ActivityNotFoundException` karena
   tidak terdaftar di manifest, lihat Known issues).
3. Izinkan permission lokasi saat diminta → pastikan kalau GPS mati, app
   mengarahkan ke halaman setting lokasi.
4. Setelah lokasi didapat: pastikan `txtLocation` terisi teks lokasi,
   `txtQiblaValue` terisi angka derajat, dan gambar kompas (`imgCompass`)
   termuat (bukan placeholder/error drawable — cek koneksi internet emulator
   kalau gagal).
5. Putar device fisik/emulator (rotasi kompas emulator lewat "Extended
   controls" > Virtual sensors kalau di emulator) → pastikan `imgCompass`
   berputar mengikuti orientasi, dan saat heading mendekati sudut kiblat,
   `qiblaAngleContainer` berubah warna (background + teks) menandakan sudah
   sejajar.
6. Cabut sinyal sensor rotasi (kalau memungkinkan) atau uji di device tanpa
   sensor rotasi untuk memverifikasi tidak ada crash (hanya diam, sesuai
   kode saat ini).

## 7. Known issues & TODOs

- [ ] `ArahKiblatActivity` (beserta `ViewPagerAdapter` dua-tab,
      `KiblatFragment`, `FalakiyahFragment`, layout `activity_arah_kiblat.xml`,
      `fragment_kiblat.xml`, `fragment_falakiyah.xml`) **tidak terdaftar di
      `AndroidManifest.xml`** dan tidak dipicu dari mana pun di codebase
      (sudah digrep di seluruh `app/src/main`, nol hasil selain deklarasi
      class-nya sendiri) — kalau dijalankan lewat `Intent` eksplisit
      (mis. saat testing/debug) akan crash `ActivityNotFoundException`.
      Kedua fragment child-nya juga masih 100% boilerplate "blank fragment"
      Android Studio, belum ada implementasi UI/logic apa pun di dalamnya.
      Kemungkinan besar ini prototipe UI ber-tab (kompas vs detail
      perhitungan falakiyah) yang ditinggalkan sebelum selesai, digantikan
      pendekatan single-screen `KiblatActivity` yang sekarang benar-benar
      dipakai. Perlu diputuskan: hapus semua file ini, atau lanjutkan
      implementasinya dan daftarkan ke manifest.
- [ ] Ada dua jalur hitung arah kiblat yang terpisah dan tidak saling
      terhubung: `KiblatActivity`/`KiblatViewModel`/`KiblatRepository`
      mengambil sudut kiblat dari **Aladhan API** (`GET v1/qibla/{lat}/{lon}`),
      sedangkan `QiblaCalculator` (rumus trigonometri bola lokal) dipakai
      hanya oleh `PrayerTimesViewModel.calculateQibla()` untuk teks detail
      rumus di layar Waktu Sholat. Tidak ada validasi bahwa kedua hasil
      konsisten satu sama lain. Kalau nanti fitur "Detail Perhitungan"
      (rencana `FalakiyahFragment` yang belum jadi) mau dibangun beneran,
      perlu diputuskan pakai sumber yang mana — atau sekalian pakai
      `QiblaCalculator` juga di `KiblatActivity` supaya konsisten.
- [ ] `getAddressFromLatLong()` di `KiblatActivity.kt` memanggil
      `Geocoder.getFromLocation()` secara **sinkron di main thread** (bukan
      lewat coroutine/`Dispatchers.IO`) — berisiko ANR terutama di device
      lama atau saat geocoder lambat merespons.
- [ ] Tidak ada penanganan eksplisit kalau device tidak memiliki sensor
      `TYPE_ROTATION_VECTOR` (`rotationSensor` bisa bernilai `null`) — kompas
      visual sekadar diam tanpa pesan/fallback apa pun ke user.
- [ ] Belum ada test otomatis sama sekali untuk fitur ini (lihat bagian
      Testing di atas).
- [ ] Ada kode rotasi kompas yang di-comment-out di
      `KiblatActivity.rotateCompassSmooth()` (percobaan animasi rotasi
      dengan `getShortestRotation`/`lastRotation` yang sudah tidak dipakai)
      — technical debt kecil, sebaiknya dibersihkan kalau file ini disentuh
      lagi.
