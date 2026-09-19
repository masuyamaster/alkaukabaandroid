# Okultasi Benda Langit

## 1. Ringkasan

**Fitur**: Okultasi Benda Langit — daftar hingga 3 konjungsi Bulan-Venus
terdekat ke depan dari lokasi markaz (GPS atau setting manual di
Konfigurasi), lengkap dengan waktu lokal mulai/puncak/berakhir kontak (kalau
memang terjadi okultasi nyata), jarak sudut minimum, dan status visibilitas
dari lokasi tersebut.

Baru mendukung **Venus** — dipicu kebutuhan mengamati okultasi Venus oleh
Bulan pada 14 September 2026. Struktur kode sengaja dibuat supaya benda
langit lain (Merkurius, bintang terang, dst.) bisa ditambah nanti lewat
parameter `body`/`bodyLabel`/`bodyRadiusKm` di `OccultationCalculator`,
tanpa mengubah model/UI.

Perhitungan 100% oleh "Astronomy Engine" (`utils/Astronomy.kt`,
`io.github.cosinekitty.astronomy`, sama seperti fitur Gerhana & Bulan
Hijriyah) — tidak ada panggilan network. Berbeda dari Gerhana, library ini
**tidak** punya fungsi pencarian okultasi siap pakai (tidak ada padanan
`lunarEclipsesAfter`/`localSolarEclipsesAfter`), jadi seluruh pencarian
konjungsi & kontak ditulis manual di atas primitif yang sudah ada
(`equator`, `horizon`, `search`).

## 2. Entry point & prasyarat

- Layar: `OkultasiActivity` (layout `activity_okultasi.xml`, judul UI
  "Okultasi Benda Langit").
- Terdaftar di `AndroidManifest.xml`, dibuka dari tombol "Okultasi"
  (`bt_okultasi`, ikon `ic_menu_star`, warna oranye
  `bg_orange_light`/`icon_orange`) di `MainActivity`/`activity_main.xml` —
  baris menu ikon di home diubah dari `weightSum="4"` jadi `weightSum="5"`
  untuk menampung tombol baru ini.
- Prasyarat: sama seperti Gerhana — permission `ACCESS_FINE_LOCATION`
  diminta on-the-fly (request code `101`, sengaja beda dari Gerhana `100`
  meski keduanya activity terpisah); kalau
  `SessionManager.isManualLocationMode()` aktif, GPS/permission dilewati dan
  lat/lng manual langsung dipakai; kalau GPS `null`/permission ditolak,
  fallback ke koordinat Jakarta (`-6.2088, 106.8456`). Ketinggian markaz
  selalu 0 m (tidak diinput manual di layar ini, sama seperti Gerhana).

## 3. Alur

1. `onCreate` -> resolve lokasi (manual/GPS/fallback) -> begitu lokasi
   didapat, langsung panggil `OkultasiViewModel.calculateOccultations(lat, lng, 0.0)`
   tanpa perlu tombol ditekan.
2. Perhitungan jalan di `Dispatchers.Default` (bukan main thread) karena
   pencarian konjungsi + kontak awal/akhir butuh banyak evaluasi posisi
   Bulan & Venus secara iteratif; `isLoading` LiveData mengontrol
   `ProgressBar`.
3. Hasil dirender ke satu `RecyclerView` (`rvOccultation`); kalau kosong
   (tidak ada konjungsi ditemukan dalam rentang pencarian), `tvEmptyState`
   ditampilkan sebagai gantinya.
4. `btnRefreshLoc` ("⟳ Ubah Lokasi") — sejak 2026-09-19 membuka sheet
   "Lokasi untuk Halaman Ini" (`PageLocationOverride`, prefs
   `OkultasiPagePrefs`): "Ikuti pengaturan global" atau "Manual khusus halaman
   ini" (isi lat/lon, GPS, atau pin di peta), persis pola Gerhana (lihat
   `gerhana.md` §2). Prioritas di `resolveLocationAndCalculate()`: override
   halaman → manual global → GPS; setelah Simpan otomatis hitung ulang.
   (Tombol ini tak lagi berfungsi sebagai "refresh GPS" satu-ketuk; pilih
   "Ikuti pengaturan global" → Simpan untuk ambil ulang.)
5. `btnBack` (toolbar) -> `finish()`.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/okultasi/OkultasiActivity.kt` + `activity_okultasi.xml` | UI: lokasi, satu RecyclerView, empty state |
| `viewmodel/okultasi/OkultasiViewModel.kt` | `LiveData<OccultationResult> result` + `LiveData<Boolean> isLoading`; jembatan Activity -> `OccultationCalculator` (dijalankan di `Dispatchers.Default` via `viewModelScope`) |
| `model/OccultationModels.kt` | `OccultationItem` (model tampilan siap-render), `OccultationResult` (saat ini cuma bungkus `venusOccultations`, disiapkan untuk body lain ke depan) |
| `utils/OccultationCalculator.kt` | Mesin hisab — lihat section 5 |
| `adapter/OccultationAdapter.kt` | Adapter `RecyclerView`, layout item `item_okultasi.xml` (pola sama dengan `LunarEclipseAdapter`/`item_gerhana_bulan.xml`) |

Alur data: lokasi resolved -> `OkultasiViewModel.calculateOccultations()` ->
`OccultationCalculator.calculate()` -> `OccultationResult` -> LiveData
`result` -> Activity bind ke `occultationAdapter`.

## 5. Mesin hisab (`OccultationCalculator`)

Karena tidak ada fungsi pencarian okultasi siap pakai di Astronomy Engine,
algoritmanya dirakit manual dari primitif yang sudah dipakai fitur lain
(`equator()`, `horizon()`, `search()` root-finder generik yang juga dipakai
`EclipseCalculator`):

1. **Posisi topocentric**: `equator(body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)`
   dipanggil untuk Bulan & Venus di observer yang sama — ini **penting**
   karena paralaks Bulan (~1°, jauh lebih besar dari paralaks Venus yang
   nyaris nol) membuat okultasi sangat bergantung lokasi pengamat, beda dari
   gerhana Bulan yang visibilitasnya cuma soal ufuk lokal.
2. **Jarak sudut**: dihitung langsung dari vektor kartesian `Equatorial.vec`
   kedua benda (`acos(dot product / (|a|*|b|))`), bukan dari RA/Dec supaya
   tidak perlu menangani wraparound RA di 24h/0h.
3. **Jari-jari sudut piringan**: `atan(radiusKm / sqrt(distanceKm² -
   radiusKm²))` — rumus yang sama dipakai `searchTransit()` di Astronomy
   Engine untuk radius topsentris. `MOON_RADIUS_KM = 1737.4` (sama dengan
   `MOON_MEAN_RADIUS_KM` di `EclipseCalculator`), `VENUS_RADIUS_KM = 6051.8`
   (konstanta yang sama dipakai `searchTransit()` untuk transit Venus).
4. **Cari konjungsi (jarak minimum)**: turunan numerik jarak sudud
   (`separationSlope`, central difference ±~29 menit) di-scan tiap 6 jam
   (`COARSE_STEP_DAYS`) sampai 2 tahun ke depan; saat tandanya berubah
   negatif->positif berarti ada minimum lokal di antara dua sampel, lalu
   `search()` (root-finder yang sama dipakai eclipse) dipakai untuk
   presisi-kan waktu puncaknya.
5. **Tentukan okultasi nyata vs cuma lewat dekat**: `isOccultation =
   jarakSudutMinimum < (radiusSudutBulan + radiusSudutVenus)` di waktu
   puncak.
6. **Waktu kontak (ingress/egress)**: kalau `isOccultation == true`, dicari
   waktu saat `jarakSudut - jumlahRadiusSudut` (disebut `contactGap`)
   menyeberang nol, di jendela ±6 jam (`CONTACT_WINDOW_DAYS`) di sekitar
   puncak — pakai `search()` lagi, sekali untuk ingress (kontak awal, tanda
   fungsi dibalik supaya jadi "akar menaik" sesuai kontrak `search()`) dan
   sekali untuk egress (kontak akhir).
7. **Visibilitas**: `horizon()` altitude Venus di waktu puncak > 0° (pola
   sama dengan badge visibilitas di Gerhana) — tidak mengecek apakah siang
   hari (lihat known limitations).

## 6. Testing

`OccultationCalculatorTest` (JVM, `app/src/test`) — pola GOLDEN/REFERENCE
TEST sama dengan `EphemerisCalculatorTest`: menjalankan `calculate()` dari
`System.currentTimeMillis()` sungguhan (bukan mock), dibandingkan ke
rentang fisis yang masuk akal + invariant algoritma.

**Temuan validasi (dijalankan 2026-09-13)**: kalkulator menemukan okultasi
Venus nyata pada **14 September 2026** (besok, relatif ke tanggal
pengujian) untuk Jakarta (masuk 19:59:49 WIB, puncak 20:15:39 WIB, keluar
20:32:25 WIB, jarak minimum 13.4') dan Sabang (jarak minimum cuma 0.4' —
okultasi dalam), tapi **tidak** terjadi kontak penuh untuk Merauke (26')
maupun Sampang (16') pada tanggal yang sama — sesuai perilaku fisis nyata
okultasi (jalur visibilitas sempit karena paralaks Bulan berubah signifikan
antar lokasi). Ini jadi bukti kuat algoritmanya benar, sekaligus
mengonfirmasi info okultasi Venus besok yang jadi alasan fitur ini dibuat.

Belum diverifikasi manual di emulator/device fisik — build (`assembleDebug`)
dan unit test sudah lolos, tapi tampilan UI aktual belum di-screenshot.

## 7. Known limitations

- [ ] Baru Venus. Body lain (Merkurius, bintang terang) belum diimplementasi
      meski struktur `findEvents(body, bodyLabel, bodyRadiusKm, ...)` sudah
      disiapkan untuk itu.
- [ ] Badge visibilitas cuma cek altitude Venus > 0° di waktu puncak, tidak
      cek apakah saat itu siang hari (Venus di atas ufuk tapi langit terang
      = sulit diamati mata telanjang tanpa alat bantu) — simplifikasi yang
      disengaja, konsisten dengan pola badge boolean tunggal di Gerhana.
- [ ] Ketinggian markaz selalu 0 m (tidak ada input manual di layar ini).
- [ ] Jendela pencarian kontak (`CONTACT_WINDOW_DAYS` = ±6 jam) berasumsi
      durasi okultasi jauh lebih pendek dari itu (secara fisis selalu benar
      untuk Bulan-Venus, durasi kontak biasanya puluhan menit) — asumsi ini
      tidak divalidasi lewat assertion eksplisit di kode, cuma lewat
      pengetahuan domain.
- [ ] Belum ada verifikasi UI manual di emulator/device fisik.
