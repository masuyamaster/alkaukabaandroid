# Masjid Terdekat

## 1. Ringkasan

**Fitur**: Masjid Terdekat — cari masjid di sekitar lokasi user saat ini,
ditampilkan sebagai daftar terurut jarak (terdekat dulu), tap salah satu untuk
buka navigasi ke sana lewat aplikasi peta eksternal.

Ide awal datang dari diskusi 2026-08-30 (dicatat di Notion, kategori "Fitur
Baru"): bantu user yang sedang bepergian menemukan masjid terdekat. Catatan
Notion eksplisit minta **cek biaya/kuota API key dulu sebelum implementasi**
(rencana awal pakai Google Maps API) — didiskusikan ulang saat implementasi
2026-09-15, hasilnya **bukan Google Maps/Places**, tapi Overpass API
(OpenStreetMap), lihat alasan di section 5.

## 2. Entry point & prasyarat

- Dari `MainActivity`: tap tombol **`btMasjidTerdekat`** (`bt_masjid_terdekat`
  di `activity_main.xml`, baris menu ketiga) → `startActivity(Intent(...,
  MasjidTerdekatActivity::class.java))`. Tidak ada extra yang dibawa lewat
  `Intent`.
- `MasjidTerdekatActivity` terdaftar di `AndroidManifest.xml`
  (`site.elahady.alkaukaba.ui.masjidterdekat.MasjidTerdekatActivity`,
  `exported="false"`).
- Prasyarat:
  - Permission runtime `ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION` +
    GPS provider aktif — pola identik dengan
    `docs/features/arah-kiblat.md` (`KiblatActivity`), termasuk bypass total
    kalau setting Lokasi di `KonfigurasiActivity` di-set ke Manual (lihat
    `docs/features/konfigurasi.md`).
  - `INTERNET` (dideklarasikan app-wide) — dipakai untuk query ke Overpass
    API. **Tidak ada API key/billing** yang dibutuhkan sama sekali (lihat
    section 5).

## 3. Titik masuk logika & navigasi

- `MasjidTerdekatActivity.checkLocationPermission()` — sama persis dengan
  `KiblatActivity`: cek `SessionManager.isManualLocationMode()` dulu, kalau
  true langsung `search(manualLat, manualLng)`; kalau tidak, minta permission
  → cek GPS aktif → `FusedLocationProviderClient` (last location, fallback
  `requestLocationUpdates` sekali kalau last location null).
- `MasjidTerdekatActivity.search(lat, lon)` — titik masuk utama logika
  pencarian: simpan `lastLat`/`lastLon`, update teks radius
  (`tvInfoPencarian`), panggil `MasjidTerdekatViewModel.search(lat, lon,
  radiusMeters)`.
- `NearbyMosqueRepository.findNearby(lat, lon, radiusMeters)` — object murni
  (bukan instance per-Activity), titik "colok" utama kalau developer lain mau
  pakai ulang logika pencarian masjid dari layar lain.
- Radius default **5 km** (`DEFAULT_RADIUS_METERS`), tombol **"Perluas Radius
  Pencarian"** (muncul di empty state) dobel radius tiap tap sampai cap
  **20 km** (`MAX_RADIUS_METERS`), lalu `search()` ulang dengan
  `lastLat`/`lastLon` yang sudah tersimpan (tidak perlu ambil lokasi lagi).
- Navigasi: `MainActivity` → `MasjidTerdekatActivity` via `Intent` biasa, satu
  arah. Tombol back pakai `view_toolbar_default` bersama
  (`binding.includeToolbar.btnBack` → `onBackPressedDispatcher.onBackPressed()`).
  Tap item masjid di list → **bukan** Activity/layar baru, tapi `Intent`
  eksternal `ACTION_VIEW` dengan URI `geo:0,0?q=lat,lon(nama)` untuk membuka
  aplikasi peta apa pun yang terpasang di device (Google Maps, atau
  alternatif lain) - lihat `MasjidTerdekatActivity.openInMaps()`.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/masjidterdekat/MasjidTerdekatActivity.kt` | Activity satu-satunya untuk fitur ini. Urus permission lokasi/GPS/mode manual (pola sama `KiblatActivity`), state radius, observe ViewModel, render list/loading/empty state, buka Intent peta eksternal saat item di-tap |
| `viewmodel/masjidterdekat/MasjidTerdekatViewModel.kt` | `search(lat, lon, radiusMeters)` di `viewModelScope`, expose `LiveData<Resource<List<NearbyMosque>>> result` |
| `repo/masjidterdekat/NearbyMosqueRepository.kt` | `findNearby()` — bangun query Overpass QL, panggil endpoint (dengan fallback multi-instance, lihat section 5), map `OverpassElement` → `NearbyMosque` (hitung jarak pakai `android.location.Location.distanceBetween`), sort ascending jarak |
| `api/OverpassApiService.kt` (`OverpassApi` interface + `OverpassRetrofitClient`) | Retrofit service khusus Overpass, base URL `https://overpass-api.de/` dengan fallback `https://overpass.kumi.systems/` — **terpisah** dari `RetrofitClient` (Aladhan) yang dipakai fitur Waktu Sholat/Arah Kiblat |
| `model/NearbyMosque.kt` | Data class hasil akhir: `id`, `name`, `address` (nullable), `latitude`, `longitude`, `distanceMeters` |
| `adapter/NearbyMosqueAdapter.kt` | `RecyclerView.Adapter` sederhana (pola sama `DoaCategoryAdapter`), format jarak (`"450 m"` di bawah 1 km, `"%.1f km"` di atas) |
| `res/layout/activity_masjid_terdekat.xml` | Toolbar + `tvInfoPencarian` (teks radius) + `SwipeRefreshLayout`+`RecyclerView` + `ProgressBar` + `layoutEmptyState` (`tvEmptyState` + tombol `btnPerluasRadius`) — pola sama `activity_daftar_kategori_doa.xml` |
| `res/layout/item_nearby_mosque.xml` | Satu baris list: ikon masjid (`ic_menu_mosque`, lingkaran `bg_sky_light`), nama, alamat (opsional, disembunyikan kalau tidak ada tag `addr:*` di OSM), pill jarak (`bg_pill_sky`), chevron |
| `res/drawable/ic_menu_mosque.xml` | Vector baru — siluet masjid (kubah + 2 menara + pintu lengkung dipotong `evenOdd` + bulan sabit kecil), dipakai juga sebagai ikon menu Beranda |

Alur data: `MainActivity` tap `btMasjidTerdekat` → `MasjidTerdekatActivity`
→ `checkLocationPermission()` → **cabang setting lokasi** (Manual vs
GPS/Otomatis, sama seperti `KiblatActivity`) → `search(lat, lon)` →
`MasjidTerdekatViewModel.search()` (`viewModelScope`) →
`NearbyMosqueRepository.findNearby()` → Overpass API (query Overpass QL
`node`/`way` dengan tag `amenity=place_of_worship` + `religion=muslim`,
radius `around:$radiusMeters,$lat,$lon`, `out center;`) → `LiveData
Resource<List<NearbyMosque>>` → observer di Activity: `Loading` (progress
bar), `Success` dengan list kosong (empty state + tombol perluas radius),
`Success` dengan data (isi `RecyclerView`), atau `Error` (empty state dengan
pesan error, atau `Toast` kalau masih ada data lama yang bisa ditampilkan).

## 5. Dependencies & tech stack khusus

- **Overpass API (OpenStreetMap)**, bukan Google Places/Maps — ini **keputusan
  sadar**, didiskusikan eksplisit sebelum implementasi (lihat catatan di
  Notion soal cek biaya API dulu). Alasan: Overpass gratis total, tanpa API
  key/billing GCP sama sekali, cukup untuk kasus pakai "list masjid + buka di
  aplikasi peta" (bukan peta interaktif in-app). Trade-off: kelengkapan data
  masjid tergantung kontribusi mapper OSM lokal (biasanya cukup baik untuk
  masjid di Indonesia, tapi tidak sekomplet Google Places untuk metadata
  seperti rating/foto/jam buka - fitur ini memang tidak menampilkan itu).
- **Retrofit + Gson** ke Overpass — sama seperti fitur lain (Aladhan API),
  tapi `RetrofitClient`/base URL terpisah (lihat section 4), karena target
  API beda total.
- **Multi-endpoint fallback**: saat fitur ini dibangun (2026-09-15), instance
  utama `overpass-api.de` sempat balas **406 Not Acceptable** untuk *semua*
  request (sudah dicek juga lewat `curl` langsung, di luar app - bukan bug
  spesifik app ini, kemungkinan masalah sementara di sisi server/jaringan),
  sementara mirror publik `overpass.kumi.systems` normal (dicek lewat `curl`,
  balas data JSON valid berisi masjid sungguhan). `OverpassRetrofitClient.instances`
  sekarang daftar (bukan satu instance), `NearbyMosqueRepository.findNearby()`
  coba tiap endpoint berurutan sampai ada yang berhasil. Kalau semua gagal,
  error dari percobaan **terakhir** yang ditampilkan ke user.
- **Timeout HTTP diperpanjang** (`OverpassRetrofitClient.httpClient`:
  connect 15s, read 30s, write 15s - default OkHttp cuma 10s/10s/10s) karena
  query Overpass (radius besar, dua tipe elemen node+way) bisa makan beberapa
  detik. Lihat known issue di section 7 soal verifikasi timeout ini di
  emulator - masih belum berhasil dicek end-to-end karena kendala jaringan
  environment development, bukan karena kodenya salah.
- **Google Play Services Location** (`FusedLocationProviderClient`) — sama
  seperti fitur Arah Kiblat/Beranda, ambil lokasi GPS.
- `android.location.Location.distanceBetween()` — bagian Android SDK bawaan,
  hitung jarak lurus (great-circle) antara user dan tiap masjid, dipakai juga
  untuk sorting hasil.
- Navigasi ke aplikasi peta eksternal pakai `Intent.ACTION_VIEW` + URI
  `geo:` standar Android (bukan deep-link Google Maps khusus) — supaya
  bekerja dengan aplikasi peta apa pun yang user punya, bukan cuma Google
  Maps.

## 6. Testing

Belum ada test otomatis untuk fitur ini (belum ada file yang menyebut
"Masjid"/"Mosque"/"Overpass" di `app/src/test`/`app/src/androidTest`).
Verifikasi saat ini manual:

1. Build & install debug APK (lihat `CLAUDE.md` root untuk perintah
   `gradlew`), pastikan `compileDebugKotlin` bersih (sudah dicek 2026-09-15,
   sukses tanpa warning baru selain warning lama yang sudah ada sebelumnya).
2. Buka `MainActivity` → tap tombol **Masjid Terdekat** (`btMasjidTerdekat`)
   → pastikan masuk ke `MasjidTerdekatActivity` (**sudah dicek** 2026-09-15
   di emulator lewat `adb shell input tap` + `dumpsys activity activities`,
   `topResumedActivity` berpindah ke `MasjidTerdekatActivity` dengan benar).
3. Kalau permission lokasi belum pernah diberikan: pastikan dialog permission
   muncul, dan kalau ditolak, `layoutEmptyState` tampil dengan pesan yang
   jelas (bukan crash/layar kosong tanpa penjelasan).
4. Setelah lokasi didapat: pastikan `tvInfoPencarian` terisi teks radius
   (**sudah dicek** 2026-09-15, menampilkan "Radius 5 km dari lokasi Anda
   saat ini"), dan list terisi hasil masjid terdekat terurut jarak menaik.
   **Belum berhasil dicek end-to-end di emulator** — lihat known issue di
   section 7 soal `SocketTimeoutException` ke kedua endpoint Overpass yang
   kejadian spesifik di environment development sesi ini.
5. Tap salah satu item masjid → pastikan aplikasi peta eksternal terbuka
   (Google Maps atau default map app emulator) mengarah ke koordinat masjid
   tersebut. **Belum dicoba interaktif** (batasi percobaan tap sesuai catatan
   di `CLAUDE.md` root soal jangan spam adb tap/screenshot).
6. Kalau hasil kosong (radius terlalu kecil/area minim data OSM): pastikan
   `layoutEmptyState` + tombol **"Perluas Radius Pencarian"** tampil, tap
   tombol itu → radius dobel (5 km → 10 km → 20 km, lalu mentok di 20 km) dan
   `tvInfoPencarian` ikut update.
7. Ganti **Konfigurasi → Lokasi → Manual** dengan koordinat tertentu → buka
   `MasjidTerdekatActivity` → pastikan **tidak ada** dialog permission GPS
   yang muncul, `tvInfoPencarian` tetap terisi radius seperti biasa (memakai
   koordinat manual). **Belum dicoba** — perlu verifikasi manual berikutnya.

## 7. Known issues & TODOs

- [ ] **List masjid belum berhasil diverifikasi terisi data asli di
      emulator** — waktu development (2026-09-15): instance utama
      `overpass-api.de` sempat balas 406 untuk semua request (dicek juga
      lewat `curl` langsung di luar app), ditambahkan fallback ke
      `overpass.kumi.systems` + timeout OkHttp diperpanjang (15s/30s/15s).
      Setelah itu, percobaan di emulator sesi ini selalu berakhir
      `java.net.SocketTimeoutException: timeout` (log lengkap: gagal di
      `Http1ExchangeCodec.readResponseHeaders` - koneksi TCP jalan, tapi
      server tidak pernah balas header dalam batas waktu) untuk **kedua**
      endpoint. Aladhan API (fitur lain, base URL beda) tetap sukses di
      emulator/sesi yang sama persis pada waktu yang sama, jadi ini bukan
      masalah internet/DNS umum di device. Overpass QL & parsing JSON-nya
      sendiri **sudah dipastikan benar** lewat `curl` manual (di luar
      app/emulator, host machine yang sama) - `overpass.kumi.systems` balas
      data masjid sungguhan valid untuk query yang persis sama dengan yang
      dikirim app. Dugaan kuat: masalah jalur jaringan spesifik dari
      environment development sesi ini (kemungkinan `curl` dari tool Bash
      jalan lewat jalur network berbeda dari traffic emulator Android yang
      sesungguhnya) ke kedua host Overpass tersebut, bukan bug di kode.
      **Perlu dicoba lagi di device/jaringan lain** (device fisik, jaringan
      rumah/kantor biasa, bukan dari sesi development ini) untuk memastikan
      fitur ini benar-benar jalan di kondisi normal sebelum dianggap selesai
      100%.
- [ ] Tidak ada retry/backoff selain fallback endpoint - kalau **kedua**
      instance Overpass gagal (mis. device offline), pesan error dari
      percobaan terakhir saja yang ditampilkan, tidak ada gabungan info dari
      kedua percobaan.
- [ ] Radius pencarian tidak bisa diatur manual oleh user (cuma dobel via
      tombol "Perluas Radius Pencarian" di empty state) - tidak ada slider
      atau input radius langsung.
- [ ] Alamat (`tvAlamatMasjid`) hanya terisi kalau OSM punya tag
      `addr:street`/`addr:suburb`/`addr:city` untuk node/way tersebut - buat
      banyak masjid kecil yang cuma punya tag minimal (`amenity`, `religion`,
      kadang tanpa `name`), baris alamat akan disembunyikan, cuma nama +
      jarak yang tampil (fallback nama masjid tanpa tag `name` adalah string
      generik `"Masjid"`).
- [ ] Belum ada test otomatis sama sekali untuk fitur ini (lihat bagian
      Testing di atas).
- [ ] `Geocoder`/reverse-geocode **tidak dipakai** di fitur ini (beda dari
      Arah Kiblat) - teks lokasi cuma bilang "radius sekian km dari lokasi
      Anda", bukan nama tempat. Ini keputusan sengaja untuk MVP (lebih
      sederhana), bisa dipertimbangkan ditambah kalau user butuh konfirmasi
      lokasi yang lebih jelas.
