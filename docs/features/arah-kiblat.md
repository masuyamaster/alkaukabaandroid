# Arah Kiblat

## 1. Ringkasan

**Fitur**: Arah Kiblat — kompas visual yang menunjukkan arah kiblat dari lokasi
user saat ini.

User membuka layar ini untuk melihat arah kiblat lewat kompas yang berputar
mengikuti orientasi fisik HP (sensor rotasi), dibandingkan dengan sudut kiblat
hasil hitung berdasarkan lokasi GPS. Saat heading HP sejajar dengan sudut
kiblat (dalam toleransi tertentu), tampilan kompas berubah warna sebagai
penanda "sudah pas".

Sejak update ini, badge "Qibla Angle" juga bisa di-tap untuk membuka **Detail
Perhitungan** — breakdown manual (Al Hasib - Alkaukaba Team) yang menjelaskan
"angka ini didapat dari mana", mengikuti rumus segitiga bola dari kertas
"Perhitungan Arah Qiblat (Dengan Teori Segitiga Siku-Siku)" - M. Khoirul Anam.
Ini pola yang sama dengan breakdown Ephemeris di fitur Waktu Sholat (lihat
`docs/features/waktu-sholat.md`), tapi lebih sederhana: Arah Kiblat cuma satu
hasil (bukan per-waktu-sholat), jadi tidak butuh registry/provider seperti di
sana - cukup satu fungsi kalkulasi langsung.

**Update 2026-08-30 — modernisasi visual kompas & info card**: kompas gambar
vintage (dimuat dari endpoint gambar Aladhan lewat Glide) diganti dua layer
vector drawable flat/minimalis yang dirotasi manual di kode (lihat section 3 &
4) — sekaligus **menghapus dependensi jaringan dari elemen visual kompas** dan
menutup known-issue lama soal kompas placeholder di mode Sumber Kiblat Manual
(section 7). Badge sudut kiblat + baris lokasi (dulu dua "pill" terpisah, satu
outline hijau satu solid navy) disatukan jadi satu **Card** (`infoCard`,
`bg_card_rounded` + tint `waktu_sholat_dark_bg`) — komponen & warna yang sama
persis dengan Hero Card di `docs/features/waktu-sholat.md`, supaya kedua layar
konsisten.

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
    terbuka tanpa data lokasi. **Kecuali** kalau setting Lokasi di
    `KonfigurasiActivity` di-set ke Manual (lihat `docs/features/konfigurasi.md`)
    — permission & GPS dilewati sama sekali, lihat section 3.
  - GPS provider harus aktif — `checkGpsEnabled()` cek
    `LocationManager.GPS_PROVIDER`; kalau mati, user diarahkan ke
    `Settings.ACTION_LOCATION_SOURCE_SETTINGS` (tidak otomatis lanjut, user
    harus balik manual ke app). Sama seperti poin di atas, ini juga dilewati
    total di mode Lokasi Manual.
  - Sensor rotasi perangkat: `Sensor.TYPE_ROTATION_VECTOR` (kompas HP). Tidak
    ada permission Android untuk ini (bukan *dangerous permission*), tapi
    kalau device tidak punya sensor tersebut `rotationSensor` bernilai `null`
    dan kompas visual sekadar diam — tidak ada pesan error ke user untuk
    kasus ini (lihat Known issues).
  - `INTERNET` (dideklarasikan app-wide di manifest) — dipakai untuk request
    sudut kiblat ke Aladhan API (`ALADHAN` source). **Sejak update
    2026-08-30**, kompas visual sendiri sudah tidak butuh internet sama
    sekali — dial & jarum dirotasi dari `qiblaAngle` yang sudah ada di memori,
    bukan gambar yang di-fetch (lihat section 3 & 4).

## 3. Titik masuk logika & navigasi

- `SessionManager.isManualLocationMode()` — dicek pertama di
  `checkLocationPermission()`. Kalau true, langsung panggil
  `onLocationReady(sessionManager.getManualLat(), sessionManager.getManualLng())`
  dan skip seluruh alur GPS/permission (lihat `docs/features/konfigurasi.md`
  untuk asal setting ini — ini setting global, dipakai juga oleh
  `MainActivity` dan `WaktuSholatActivity`).
- `SessionManager.getQiblaSource()` (`ALADHAN` default, atau
  `MANUAL_FORMULA`) — dicek di `onLocationReady()`, menentukan sumber mana
  yang jadi acuan **utama** (`txtQiblaValue` + alignment kompas):
  - `ALADHAN`: seperti sebelumnya, panggil `viewModel.fetchQiblaAngle(lat, lon)`.
  - `MANUAL_FORMULA`: **tidak** memanggil `fetchQiblaAngle` sama sekali
    (sengaja — kalau tetap dipanggil, response Aladhan yang datang belakangan
    lewat observer akan menimpa balik nilai manual, race condition). Sudut
    `qiblaAngle`/`txtQiblaValue` diisi langsung dari
    `qiblaBreakdown.utsbDegree`.
  - **Sejak update 2026-08-30**, `loadQiblaCompass()` (dan Glide) sudah
    dihapus total — kompas visual (`imgCompassDial` + `imgCompassNeedle`)
    dirotasi langsung dari variabel `qiblaAngle`/`currentAzimuth` di
    `rotateCompassSmooth()` (lihat alur data di bawah), jadi baik source
    `ALADHAN` maupun `MANUAL_FORMULA` sama-sama dapat jarum yang akurat —
    tidak ada lagi cabang khusus/placeholder generik untuk mode Manual.
- `KiblatViewModel.fetchQiblaAngle(lat, lon)` — dipanggil dari
  `KiblatActivity.onLocationReady()` begitu lokasi didapat **dan** sumber
  aktif adalah `ALADHAN` (lihat poin di atas). Hasilnya di-expose lewat
  `LiveData<Double> qiblaAngle` (derajat, dari API) dan `LiveData<String> error`.
- `KiblatRepository.getQiblaAngle(lat, lon)` — jembatan ke
  `AladhanApi.getQiblaDirection(lat, lon)` (endpoint Aladhan
  `GET v1/qibla/{latitude}/{longitude}`), balikin `.data.direction`.
- `QiblaCalculator.calculateBreakdown(userLat, userLong)` — util murni
  (object), hitung breakdown manual arah kiblat pakai rumus
  `tan⁻¹(cos φ × tan φ_Kabah ÷ sin C − sin φ ÷ tan C)` (kertas Al Hasib).
  **Sekarang dipakai langsung oleh `KiblatActivity`** (dipanggil di
  `onLocationReady()`, hasilnya disimpan di properti `qiblaBreakdown` dan
  ditampilkan lewat `showQiblaBreakdownSheet()` saat badge sudut di-tap).
  Sebelumnya kelas ini (dengan nama fungsi lama `calculateQibla()`) dipakai
  `PrayerTimesViewModel` untuk teks detail di layar Waktu Sholat — itu sudah
  dihapus (lihat `docs/features/waktu-sholat.md`, bagian "Kenapa Arah Kiblat
  dikeluarkan") karena keliru secara scope. Sekarang `QiblaCalculator` cuma
  dipakai di fitur Arah Kiblat sendiri, tempat yang seharusnya.
- Navigasi: `MainActivity` → `KiblatActivity` via `Intent` biasa, satu arah,
  tanpa extra. Tombol back pakai `view_toolbar_default` bersama
  (`binding.includeToolbar.btnBack.setOnClickListener`) memanggil
  `onBackPressedDispatcher.onBackPressed()`. Di dalam layar ini, tap Card
  `infoCard` (dulu bernama `qiblaAngleContainer`, sekarang satu Card penuh —
  lihat Update 2026-08-30 di section 1) atau ikon kecil `btnQiblaDetail` di
  dalamnya membuka `BottomSheetDialog` (`dialog_qibla_breakdown.xml`) — modal,
  bukan Activity terpisah, sama seperti pola bottom sheet di
  `KonfigurasiActivity`.
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
| `ui/arahkiblat/KiblatActivity.kt` | Activity aktif satu-satunya untuk fitur ini (terdaftar di manifest, dipicu dari `MainActivity`). Urus permission lokasi, cek GPS aktif, ambil lokasi via `FusedLocationProviderClient`, dengarkan sensor rotasi, rotasikan dial+jarum kompas vector lokal (bukan Glide lagi), hitung alignment heading vs sudut kiblat |
| `ui/arahkiblat/ArahKiblatActivity.kt` | Activity alternatif ber-tab (`ViewPager`) — **tidak terdaftar di manifest, dead code**, lihat Known issues |
| `ui/arahkiblat/KiblatFragment.kt` + `res/layout/fragment_kiblat.xml` | Tab pertama `ArahKiblatActivity` — masih boilerplate "blank fragment" bawaan Android Studio (`TODO: Update blank fragment layout`, teks `hello_blank_fragment`), tidak ada implementasi |
| `ui/arahkiblat/FalakiyahFragment.kt` + `res/layout/fragment_falakiyah.xml` | Tab kedua `ArahKiblatActivity` ("Detail Perhitungan") — sama, masih blank fragment boilerplate, tidak ada implementasi |
| `viewmodel/arahkiblat/KiblatViewModel.kt` | `fetchQiblaAngle(lat, lon)` di `viewModelScope`, expose `qiblaAngle`/`error` sebagai `LiveData` |
| `viewmodel/arahkiblat/KiblatViewModelFactory.kt` | DI manual (tanpa framework) — construct `KiblatRepository(RetrofitClient.instance)` lalu `KiblatViewModel` |
| `repo/arahkiblat/KiblatRepository.kt` | `getQiblaAngle()` — satu-satunya pemanggil `AladhanApi.getQiblaDirection` |
| `api/PrayersApiService.kt` (`AladhanApi` interface + `RetrofitClient`, base URL `https://api.aladhan.com/`) | Retrofit service bersama, dipakai juga oleh fitur Waktu Sholat (lihat `docs/features/waktu-sholat.md`) |
| `utils/QiblaCalculator.kt` | Util murni, hitung breakdown manual arah kiblat (rumus Al Hasib). Dipakai langsung oleh `KiblatActivity` |
| `utils/SessionManager.kt` | Sumber setting global: `isManualLocationMode()`/`getManualLat()`/`getManualLng()` (lokasi) dan `getQiblaSource()` (Aladhan vs Rumus Manual) — diisi lewat `KonfigurasiActivity`, dibaca di sini |
| `res/layout/activity_kiblat.xml` | Layout `KiblatActivity`: toolbar (`view_toolbar_default`), `imgCompassDial` + `imgCompassNeedle` (dua layer vector, dirotasi independen), `calibrationHint`, dan Card `infoCard` (`clickable`) berisi `txtQiblaLabel`/`txtQiblaValue`/`btnQiblaDetail`/`txtLocation` |
| `res/drawable/ic_compass_dial_modern.xml` | Vector dial kompas: ring tipis, tick derajat tiap 15°, marker segitiga merah "Utara" di atas. Dirotasi `-currentAzimuth` |
| `res/drawable/ic_qibla_needle.xml` | Vector jarum kiblat, satu panah tebal warna `waktu_sholat_dark_bg` (biru dongker, sama dengan Hero Card Waktu Sholat). Dirotasi `qiblaAngle - currentAzimuth`, independen dari dial |
| `res/layout/dialog_qibla_breakdown.xml` | Bottom sheet Detail Perhitungan — judul + disclosure singkat + container untuk baris breakdown |
| `res/layout/item_breakdown_row.xml` | Satu baris label/value di breakdown — **dipakai bersama** dengan fitur Waktu Sholat (lihat `docs/features/waktu-sholat.md`), bukan file baru khusus Kiblat |
| `res/layout/activity_arah_kiblat.xml` | Layout `ArahKiblatActivity` (tab layout + `ViewPager`) — hanya dipakai Activity yang dead code |

Alur data (sudut kiblat numerik, `txtQiblaValue`): `KiblatActivity.onCreate()`
→ `checkLocationPermission()` → **cabang setting lokasi (lihat section 3)**:
  - Manual: langsung `onLocationReady(manualLat, manualLng)`.
  - Otomatis: `checkGpsEnabled()` → `getLastLocation()`/`requestNewLocation()`
    (via `FusedLocationProviderClient`) → `onLocationReady(lat, lon)`.

Dari `onLocationReady(lat, lon)`, **cabang setting sumber kiblat** (section 3):
  - `ALADHAN`: `viewModel.fetchQiblaAngle(lat, lon)` → `KiblatViewModel`
    (coroutine di `viewModelScope`) → `KiblatRepository.getQiblaAngle()` →
    `AladhanApi.getQiblaDirection(lat, lon)` → `.data.direction` →
    `LiveData qiblaAngle` → observer di Activity set `binding.txtQiblaValue.text`
    **dan** simpan ke variabel `qiblaAngle: Float` (dipakai alignment kompas).
  - `MANUAL_FORMULA`: `qiblaAngle` & `txtQiblaValue.text` diisi langsung dari
    `qiblaBreakdown.utsbDegree` (hasil `QiblaCalculator.calculateBreakdown`
    yang sudah dihitung tepat sebelumnya) — `fetchQiblaAngle` tidak dipanggil.

Alur data (kompas visual, `imgCompassDial` + `imgCompassNeedle`) — **diubah
total di update 2026-08-30**: tidak ada lagi fetch gambar dari network sama
sekali (Glide, `loadQiblaCompass()`, `ic_compass_placeholder`,
`ic_compass_error` semuanya dihapus). Dua `ImageView` bertumpuk di
`contentContainer`, masing-masing pakai vector drawable statis lokal, dirotasi
lewat property `.rotation` murni di `rotateCompassSmooth()` (lihat alur
heading di bawah) berdasarkan `qiblaAngle` yang sudah tersedia di memori
Activity — sehingga berlaku identik untuk source `ALADHAN` maupun
`MANUAL_FORMULA`, tidak ada percabangan lagi seperti sebelumnya.

Alur data (heading/orientasi HP): `onResume()` daftarkan
`sensorListener` ke `Sensor.TYPE_ROTATION_VECTOR` (`SENSOR_DELAY_GAME`) →
`onSensorChanged()` hitung rotation matrix → azimuth mentah → dihaluskan
(`unwrapAngle()` + `lowPassFilter()`, faktor `0.15f`) jadi `currentAzimuth` →
`rotateCompassSmooth()`:
  - `imgCompassDial.rotation = -currentAzimuth` — dial (ring + tick + marker
    "Utara") berputar mengikuti heading device, supaya marker Utara tetap
    akurat terhadap utara geografis.
  - `imgCompassNeedle.rotation = qiblaAngle - currentAzimuth` — jarum kiblat
    dihitung **independen** dari dial, langsung dari selisih sudut kiblat vs
    heading saat ini, jadi selalu menunjuk kiblat relatif ke layar tanpa
    tergantung gambar network apa pun.
  - lalu panggil `checkQiblaAlignment()`, yang membandingkan `currentAzimuth`
    vs `qiblaAngle` dengan threshold `3°` (`qiblaThresshold`) — kalau selaras,
    `txtQiblaValue` (di dalam `infoCard`) berganti warna teks jadi
    `accent_yellow` (emas), kalau tidak kembali ke putih. **Beda dari
    sebelumnya**: dulu seluruh `Drawable` background badge diganti
    (`bg_qibla_match`/`bg_qibla_angle`, alokasi objek baru tiap event sensor —
    lihat Known issues lama), sekarang cukup `setTextColor()` memakai warna
    yang sudah pasti ter-resolve, jadi TODO alokasi berlebih itu otomatis
    tertutup.
`onAccuracyChanged()` juga menampilkan/menyembunyikan `calibrationHint` saat
akurasi sensor rendah/unreliable — tidak berubah dari sebelumnya.

Alur data (breakdown manual, "Detail Perhitungan"): `onLocationReady()` juga
panggil `QiblaCalculator.calculateBreakdown(lat, lon)` — **selalu**, terlepas
dari sumber kiblat aktif apa (dipakai untuk isi sheet breakdown, dan kalau
sumbernya `MANUAL_FORMULA` juga dipakai sebagai `qiblaAngle` utama, lihat di
atas). Hasilnya (`QiblaBreakdownResult`) disimpan di properti `qiblaBreakdown`
(bukan `LiveData` — cukup properti biasa karena cuma dibaca sekali saat tap,
tidak perlu observasi berkelanjutan). Tap `infoCard`/`btnQiblaDetail`
→ `showQiblaBreakdownSheet()` → kalau `qiblaBreakdown` masih `null` (lokasi
belum siap), tampilkan `Toast`; kalau sudah ada, set teks subtitle
(`tvQiblaBreakdownSubtitle`) sesuai `sessionManager.getQiblaSource()` (jelaskan
ke user apakah angka di breakdown ini juga yang jadi acuan utama layar, atau
cuma referensi), lalu inflate `item_breakdown_row.xml` untuk tiap baris
(Lintang/Bujur Ka'bah, Lintang/Bujur lokasi, Selisih Bujur, Rumus, hasil
B-U/U-B/UTSB) ke dalam `BottomSheetDialog` (`dialog_qibla_breakdown.xml`).

**Kapan breakdown ini "cuma referensi" vs "jadi acuan utama"**: kalau sumber
kiblat aktif `ALADHAN`, breakdown manual di sini murni pembanding — angka
`txtQiblaValue`/kompas tetap dari Aladhan API, dan **tidak dijamin identik**
dengan breakdown (beda sumber/presisi data matahari & Ka'bah, contoh nyata:
untuk Lamongan kompas API pernah menunjukkan 294° sementara breakdown lokal
294°04'39"). Kalau sumber aktif `MANUAL_FORMULA`, keduanya **selalu identik**
karena `txtQiblaValue` memang diisi dari angka breakdown yang sama. Lihat
`docs/features/konfigurasi.md` untuk cara ganti setting ini.

Alur data (teks lokasi): `onLocationReady()` cek `isManualLocationMode()`
dulu — kalau true, `binding.txtLocation.text` diisi langsung dari koordinat
manual (`"Lokasi manual: %.4f, %.4f"`), **tidak** memanggil geocoder sama
sekali. Kalau false (mode Otomatis/GPS), baru panggil `getAddressFromLatLong
(lat, lon)` seperti sebelumnya — `Geocoder.getFromLocation()` dipanggil
**sinkron di main thread** (bukan di coroutine/background thread), hasilnya
langsung di-set ke `binding.txtLocation`. **Bug yang sudah diperbaiki**:
sebelum ada cabang Manual ini, `getAddressFromLatLong()` dipanggil untuk
koordinat manapun (termasuk titik manual yang jarang punya nama jalan/desa
resmi), dan karena fungsinya pakai interpolasi string polos
`"$kecamatan, $kota, $negara"` (bukan `listOfNotNull(...).joinToString()`
seperti di `MainViewModel.fetchAddressName()`), hasil `null` dari Geocoder
tercetak literal jadi teks **"null, null, Indonesia"** di UI kalau
`subLocality`/`locality` tidak resolve.

## 5. Dependencies & tech stack khusus

- **Glide** — **sejak update 2026-08-30 tidak dipakai lagi di fitur ini**
  (dulu untuk load gambar kompas dari URL Aladhan lewat `loadQiblaCompass()`,
  sekarang kompas visual pakai vector drawable lokal yang dirotasi manual,
  lihat section 3 & 4). Cek dependency Glide di `build.gradle` sebelum
  dihapus total dari project — kemungkinan masih dipakai fitur lain (mis.
  foto profil).
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
   `txtQiblaValue` (di dalam Card `infoCard`) terisi angka derajat, dan dial +
   jarum kompas (`imgCompassDial`/`imgCompassNeedle`) langsung tampil (vector
   lokal, tidak butuh koneksi internet lagi untuk elemen visual ini).
5. Putar device fisik/emulator (rotasi kompas emulator lewat "Extended
   controls" > Virtual sensors kalau di emulator) → pastikan `imgCompassDial`
   berputar mengikuti orientasi (marker segitiga merah "Utara" tetap akurat),
   `imgCompassNeedle` menunjuk kiblat secara independen, dan saat heading
   mendekati sudut kiblat, warna `txtQiblaValue` berubah jadi emas
   (`accent_yellow`) menandakan sudah sejajar.
6. Cabut sinyal sensor rotasi (kalau memungkinkan) atau uji di device tanpa
   sensor rotasi untuk memverifikasi tidak ada crash (hanya diam, sesuai
   kode saat ini).
7. Tap badge "Qibla Angle" → pastikan bottom sheet "Detail Perhitungan Arah
   Kiblat" terbuka, berisi baris Lintang/Bujur Ka'bah, Lintang/Bujur lokasi,
   Selisih Bujur, Rumus, dan tiga baris hasil (B-U/U-B/UTSB).

8. Ganti **Konfigurasi → Lokasi → Manual** dengan koordinat tertentu → buka
   `KiblatActivity` lagi → pastikan **tidak ada** dialog permission GPS yang
   muncul, `txtLocation` langsung terisi `"Lokasi manual: lat, lng"` (bukan
   alamat hasil geocode), dan breakdown (`Lintang lokasi`/`Bujur lokasi`)
   menunjukkan koordinat manual yang sama (dalam format DMS).
9. Ganti **Konfigurasi → Arah Kiblat → Sumber Perhitungan** ke **Rumus Manual
   (Al Hasib)** → buka `KiblatActivity` → pastikan `txtQiblaValue` sama
   persis dengan "Hasil akhir (UTSB)" di breakdown, **dan** `imgCompassNeedle`
   tetap berputar menunjuk sudut yang sama (sejak update 2026-08-30, mode ini
   tidak lagi jatuh ke placeholder generik — lihat Known issues, item selesai).

**Catatan verifikasi sesi 2026-08-30 (modernisasi visual kompas & info card)**:
perubahan ini (dial+jarum vector, Card `infoCard`) **belum dicoba interaktif
di emulator/device** — environment sesi ini tidak punya `JAVA_HOME`/JDK
terpasang jadi `gradlew` tidak bisa dijalankan sama sekali untuk compile-check
maupun install APK. Yang sudah dipastikan: referensi id antar
`activity_kiblat.xml` ↔ `KiblatActivity.kt` sudah dicocokkan manual (baca
ulang kedua file), drawable yang dihapus (`bg_qibla_angle`, `bg_qibla_match`,
`bg_location`, `ic_compass_placeholder`, `ic_compass_error`) sudah digrep dan
dipastikan tidak dipakai file lain. **Perlu di-build & dicoba langsung di
Android Studio** sebelum dianggap selesai 100% — cek proporsi ukuran dial vs
jarum (`padding="24dp"`/`"56dp"` di kedua `ImageView`, lihat section 4) enak
dilihat di device sungguhan, dan pastikan rotasi jarum & dial tetap mulus
seperti versi gambar lama.

**Catatan verifikasi sesi 2026-08-30 (lokasi/sumber kiblat)**: poin 8 & 9 di
atas sudah dicek **berhasil** di emulator — nilai `SessionManager` di-set
langsung lewat `adb shell run-as ... cat shared_prefs/AppSession.xml` untuk
memastikan state persist benar, lalu dikonfirmasi lewat screenshot layar
Kiblat & Waktu Sholat menampilkan koordinat manual yang sama persis (format
beda: desimal di Konfigurasi vs DMS di breakdown Kiblat, sudah dicocokkan
manual dan memang sama). Bug "null, null, Indonesia" (lihat section 4)
ditemukan & diperbaiki lewat verifikasi ini.

**Catatan verifikasi sesi sebelumnya**: poin 7 sudah dicek benar lewat compile +
perhitungan manual (hasil breakdown ≈24° B-U untuk lokasi Lamongan, cocok
dengan contoh di kertas Al Hasib ≈24°04'39"), tapi **belum berhasil dicek
interaktif di emulator** — `adb shell input tap` gagal terdaftar sama sekali
di layar ini selama sesi debugging (bahkan tombol back toolbar bawaan pun
tidak merespons tap sintetis, sementara `KEYCODE_BACK` fisik berhasil).
Dugaan kuat: `sensorListener` yang update `imgCompass.rotation` +
`qiblaAngleContainer.background` di setiap event sensor (`SENSOR_DELAY_GAME`)
membuat main thread terlalu sibuk untuk memproses touch event sintetis dari
adb pada emulator ini. Belum tentu terjadi di device fisik (sensor asli
biasanya jauh lebih jarang update dibanding simulasi emulator), tapi perlu
dicoba manual di device sungguhan sebelum dianggap selesai 100%.

## 7. Known issues & TODOs

- [x] ~~Kompas visual saat Sumber Perhitungan = Manual masih placeholder
      generik~~ — **selesai (2026-08-30)**. `loadQiblaCompass()` dan Glide
      sudah dihapus total; kompas sekarang dua vector drawable
      (`ic_compass_dial_modern.xml` dial + `ic_qibla_needle.xml` jarum,
      lihat section 4) yang dirotasi langsung dari `qiblaAngle` yang sudah di
      memori, jadi akurat untuk source `ALADHAN` maupun `MANUAL_FORMULA`
      tanpa percabangan/placeholder khusus lagi.
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
- [x] ~~Ada dua jalur hitung arah kiblat yang terpisah~~ — **selesai**.
      `QiblaCalculator` sekarang dipakai langsung oleh `KiblatActivity`
      sendiri (breakdown manual lewat tap badge), bukan nyasar ke
      `PrayerTimesViewModel`/layar Waktu Sholat lagi. Sudut kompas (Aladhan
      API) dan breakdown manual (`QiblaCalculator`) tetap dua sumber angka
      yang independen (disengaja, sama seperti pola Ephemeris di Waktu
      Sholat) — bukan lagi "tidak saling terhubung secara scope", tapi
      "sengaja dua sumber, sama-sama ditampilkan di layar yang sama".
- [ ] **Verifikasi interaktif breakdown belum tuntas** — lihat catatan di
      bagian Testing poin 7. Perlu dicoba tap manual di device fisik/emulator
      lain untuk memastikan `showQiblaBreakdownSheet()` benar-benar terbuka
      saat disentuh user sungguhan, bukan cuma lewat code review + compile.
- [x] ~~`checkQiblaAlignment()` mengganti background Drawable tiap callback
      sensor~~ — **selesai (2026-08-30)** sebagai efek samping dari
      penggantian pill jadi Card `infoCard`: alignment sekarang cuma
      `txtQiblaValue.setTextColor(...)` (dua warna solid, tidak ada alokasi
      `Drawable` baru sama sekali), dipanggil dari `checkQiblaAlignment()` di
      setiap callback sensor seperti sebelumnya tapi jauh lebih murah.
- [ ] `getAddressFromLatLong()` di `KiblatActivity.kt` memanggil
      `Geocoder.getFromLocation()` secara **sinkron di main thread** (bukan
      lewat coroutine/`Dispatchers.IO`) — berisiko ANR terutama di device
      lama atau saat geocoder lambat merespons.
- [ ] Tidak ada penanganan eksplisit kalau device tidak memiliki sensor
      `TYPE_ROTATION_VECTOR` (`rotationSensor` bisa bernilai `null`) — kompas
      visual sekadar diam tanpa pesan/fallback apa pun ke user.
- [ ] Belum ada test otomatis sama sekali untuk fitur ini (lihat bagian
      Testing di atas).
- [x] ~~Ada kode rotasi kompas yang di-comment-out di
      `KiblatActivity.rotateCompassSmooth()`~~ — **selesai (2026-08-30)**,
      dibersihkan sekalian saat fungsi ini ditulis ulang untuk dial+jarum dua
      layer (lihat section 3 & 4).
