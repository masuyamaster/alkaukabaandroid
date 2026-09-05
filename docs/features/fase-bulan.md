# Fase Bulan

## 1. Ringkasan

**Fitur**: Fase Bulan — kartu di home screen yang menampilkan ilustrasi
fase bulan saat ini (real-time, berdasarkan waktu sistem device, dirender
pakai tekstur foto bulan asli — lihat §4) beserta nama fase dan persentase
permukaan yang tersinari. Tap kartu membuka layar detail dengan ilustrasi
lebih besar, kartu "Detail Astronomis" (magnitude, jarak, radius, RA/Dec,
Az/Alt, jam terbit/terbenam), dan jadwal 4 fase bulan mendatang (bulan
baru/kuartal pertama/purnama/kuartal akhir).

Sengaja ditaruh sebagai section/kartu terpisah di home, **bukan** ikon ke-5 di
baris 4-menu utama (Arah Kiblat/Waktu Sholat/Awal Bulan/Gerhana) — baris itu
`weightSum="4"` dan didesain sebagai 4 aksi cepat, bukan tempat untuk fitur
informasional/dekoratif seperti ini.

Semua perhitungan pakai "Astronomy Engine" (`utils/Astronomy.kt`,
`io.github.cosinekitty.astronomy`) yang sudah dipakai fitur Bulan Hijriyah &
Gerhana — tidak ada perhitungan astronomi baru yang ditambahkan.

## 2. Entry point & prasyarat

- Kartu "Fase Bulan Malam Ini" di `activity_main.xml` (`cardMoonPhase` /
  `btnMoonPhase`), antara baris 4-menu dan section "Kalender".
- Tap kartu → `FaseBulanActivity` (layout `activity_fase_bulan.xml`).
- Prasyarat inti (ilustrasi, nama fase, %, magnitude, jarak, radius, RA/Dec,
  fase mendatang): tidak ada — murni perhitungan waktu (tanggal & jam device)
  tanpa panggilan network.
- Prasyarat tambahan khusus kartu "Detail Astronomis" di layar detail: izin
  lokasi (`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`), dibutuhkan untuk
  Az/Alt dan jam terbit/terbenam Bulan (toposentris, beda per lokasi
  pengamat) — lihat §4. Kalau izin ditolak atau lokasi belum siap, dua field
  ini tampil "-"; field lain tidak terpengaruh.

## 3. Titik masuk logika & navigasi

- `MainActivity.setupMoonPhaseCard()` — dipanggil dari `onCreate`, hitung fase
  saat ini dan isi kartu home, pasang `OnClickListener` ke `FaseBulanActivity`.
- `FaseBulanActivity.showCurrentPhase()` — versi lebih besar dari logika yang
  sama di kartu home, ditambah magnitude/jarak/radius (geosentris, tidak
  butuh lokasi).
- `FaseBulanActivity.checkLocationPermission()` → `onLocationReady()` — alur
  izin & fetch lokasi (pola sama dengan `KiblatActivity`: cek
  `SessionManager.isManualLocationMode()` dulu, baru fallback ke
  `FusedLocationProviderClient`), lalu hitung RA/Dec topocentric, Az/Alt, dan
  jam terbit/terbenam Bulan buat kartu "Detail Astronomis".
- `FaseBulanActivity.loadUpcomingQuarters()` — cari 4 fase mendatang lewat
  `searchMoonQuarter`/`nextMoonQuarter`, dijalankan di `Dispatchers.Default`
  (iteratif/pencarian akar, dipindah dari main thread) lalu bind ke UI di
  `Dispatchers.Main`.
- `MoonPhaseView` (`ui/widget/MoonPhaseView.kt`) — custom `View` yang reusable,
  dipakai di kartu home (56dp), layar detail (160dp), dan ilustrasi hilal di
  `AwalBulanActivity`, lewat method publik
  `setPhase(phaseAngleDegrees, illuminatedFraction)`.
- `MoonPhaseLabel.forAngle()` (`utils/MoonPhaseLabel.kt`) — mapping sudut fase
  sinodik ke 8 nama fase. Per 2026-09-05, dua iterasi sebelum settle ke bentuk
  final: sempat diganti ke istilah falakiyah dari tabel "Al-Azhillah
  Al-Qamariyah" (Ijtimak, Hilal/Sabit Muda, Tarbi' Awwal, dst), lalu sempat ke
  terjemahan Indonesia (Bulan Baru, Sabit Awal, Kuartal Pertama, dst) — versi
  final memakai istilah **Fase Astronomis** apa adanya dalam Bahasa Inggris:
  New Moon, Waxing Crescent, First Quarter, Waxing Gibbous, Full Moon, Waning
  Gibbous, Last Quarter, Waning Crescent — persis kolom "Fase Astronomis" di
  [referensi-ilmu-falak.md §2](../referensi-ilmu-falak.md) (istilah falakiyah
  di kolom sebelahnya tetap didokumentasikan di sana sebagai referensi, tapi
  tidak dipakai di UI). Label statis di kartu "Fase Mendatang"
  (`activity_fase_bulan.xml`, 4 baris) mengikuti keputusan yang sama.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/widget/MoonPhaseView.kt` | Custom `View`: gambar piringan gelap penuh, lalu area terang dibentuk dari setengah lingkaran (limb terang, kiri/kanan tergantung waxing/waning) dipotong/ditambah elips terminator (dua-arc `Path`, lebar elips = `r * |1 - 2k|`, `k` = fraksi tersinari), lalu di-fill pakai `BitmapShader` dari `res/drawable-nodpi/moon_texture.jpg` (bukan warna flat) supaya terlihat foto asli. Bagian gelap tetap warna flat navy (`shadowPaint`) — sisi tak tersinari memang tidak terlihat apa pun di kenyataan. Tekstur foto ini bisa dimatikan per-instance lewat `setRealisticTexture(false)` (fallback ke `flatBrightPaint` putih flat) — dipakai `AwalBulanActivity` untuk ilustrasi hilal (Per 2026-09-05: user minta hilal tetap putih flat, bukan foto realistis; kartu Fase Bulan & home tidak berubah, tetap tekstur foto). |
| `res/drawable-nodpi/moon_texture.jpg` | Foto purnama Bulan asli, di-crop dari `File:FullMoon2010.jpg` di Wikimedia Commons — © Gregory H. Revera, lisensi CC BY-SA 3.0 (**atribusi wajib** kalau file/versi turunannya didistribusikan lagi di luar app ini; ditampilkan sebagai UI di dalam app tidak butuh watermark, tapi kredit ini harus tetap ada di sini & tidak boleh dihapus). `drawable-nodpi` supaya didekode di resolusi piksel aslinya di semua densitas device, bukan di-scale otomatis. Per 2026-09-05: sumber awal (`File:Full moon.jpeg`, NASA, domain publik) diganti karena piringan bulannya tidak simetris dalam frame (margin kiri/atas ada, kanan/bawah nol — disc-nya kepotong tepi foto), bikin celah kelihatan antara tepi foto & lingkaran gold yang digambar `MoonPhaseView`. `FullMoon2010.jpg` di-crop presisi (deteksi bounding box piksel non-hitam via kode Java sekali pakai, radius = setengah sisi terpanjang bbox, crop persegi center pas di situ) supaya piringannya nyaris pas isi seluruh frame simetris (margin ~4-5px di semua sisi pada file 900×900) — align rapi dengan lingkaran yang digambar, tanpa celah. |
| `utils/MoonPhaseLabel.kt` | 8 bucket nama fase (masing-masing 45°) dari sudut sinodik 0-360°. |
| `ui/fasebulan/FaseBulanActivity.kt` + `activity_fase_bulan.xml` | Layar detail: kartu navy besar (ilustrasi + nama fase + %), kartu putih "Detail Astronomis" (magnitude/jarak/radius/RA-Dec/Az-Alt/terbit-terbenam), lalu kartu putih daftar 4 fase mendatang. |
| `MainActivity.kt` | `setupMoonPhaseCard()` mengisi kartu home + wiring klik ke `FaseBulanActivity`. |
| `activity_main.xml` | Kartu `cardMoonPhase`/`btnMoonPhase` (gaya `bg_card_gradient_navy`, konsisten dengan kartu Gerhana). |

Alur data (fase & info geosentris saat ini, tanpa lokasi):
`Time.fromMillisecondsSince1970(now)` → `moonPhase(time)` (sudut sinodik
0-360°, untuk `MoonPhaseView` + label), `illumination(Body.Moon,
time)` (`phaseFraction` untuk ilustrasi + teks persentase, `mag` untuk
magnitude), dan `geoVector(Body.Moon, time, Aberration.Corrected).length() *
KM_PER_AU` (jarak geosentris km) → langsung bind ke UI (sinkron, tidak perlu
coroutine — perhitungan single-point sangat cepat). Radius Bulan
(`moonMeanRadiusKm = 1737.4`) konstanta fisik (IAU mean radius), tidak
dihitung ulang.

Alur data (Detail Astronomis yang butuh lokasi, dijalankan di
`Dispatchers.Default` lalu bind di `Dispatchers.Main`): dapat `lat/lon` (GPS
atau lokasi manual dari `SessionManager`) → `Observer(lat, lon, 0.0)` →
`equator(Body.Moon, now, observer, EquatorEpoch.OfDate, Aberration.Corrected)`
(RA/Dec topocentric) → `horizon(now, observer, eq.ra, eq.dec,
Refraction.Normal)` (Az/Alt) → `searchRiseSet(Body.Moon, observer,
Direction.Rise/Set, now, 1.2)` (jam terbit/terbenam terdekat ke depan). RA
diformat jam sideris (`HHhMMmSS.Ss`), Dec/Az/Alt diformat derajat-menit-detik
(`DD°MM'SS.S"`) lewat helper lokal `formatRaHours`/`formatDegreesDms` di
`FaseBulanActivity`.

Alur data (4 fase mendatang): `searchMoonQuarter(now)` → `nextMoonQuarter()`
dipanggil 3x berantai → 4 `MoonQuarterInfo` (quarter 0=baru, 1=kuartal
pertama, 2=purnama, 3=kuartal akhir, berurutan kronologis) → format tanggal
Indonesia → bind ke 4 `TextView` di kartu "Fase Mendatang".

**Catatan penting — dua "phase angle" yang berbeda di Astronomy Engine, jangan
tertukar:**
- `IlluminationInfo.phaseAngle` (dari `illumination()`) = sudut Matahari-Bumi
  dilihat dari Bulan, **0°=purnama, 180°=baru, rentang 0-180, simetris** — TIDAK
  bisa dipakai untuk membedakan waxing vs waning (nilainya sama persis di kedua
  sisi siklus).
- `moonPhase(time)` = sudut sinodik geosentris Bulan relatif Matahari,
  **0°=baru, 90°=kuartal pertama, 180°=purnama, 270°=kuartal akhir, rentang
  0-360** — inilah yang dipakai `MoonPhaseView`/`MoonPhaseLabel` untuk arah
  limb terang & nama fase.

Sempat salah pakai `IlluminationInfo.phaseAngle` di iterasi pertama (2026-09-05)
— hasilnya nama fase & sisi terang salah/tidak konsisten dengan persentase
tersinari. Sudah diperbaiki ke `moonPhase()`.

## 5. Dependencies & tech stack khusus

- Tidak ada tambahan library baru. `BitmapShader`/`Matrix` (tekstur foto
  bulan) dan `Path.arcTo` (bentuk sabit/cembung) adalah API
  `android.graphics` bawaan platform — bukan dependency eksternal.
- Satu aset raster baru: `res/drawable-nodpi/moon_texture.jpg` (~245KB, foto
  CC BY-SA 3.0 — wajib atribusi, lihat §4). Ini raster pertama di luar
  logo/mipmap-xxxhdpi; sebelumnya semua drawable di repo ini vector.
- Lokasi (§2) pakai `com.google.android.gms.location` (`FusedLocationProviderClient`),
  sudah jadi dependency existing lewat fitur Arah Kiblat — tidak ada
  penambahan library baru untuk itu juga.

## 6. Testing

Tidak ada test otomatis. Verifikasi manual dilakukan build debug APK →
install ke emulator `Pixel6_API34` (bukan device fisik, sesuai preferensi
testing project ini) → cek kartu home render ilustrasi (tekstur foto,
bukan flat color) + label + persentase konsisten, tap kartu → cek layar
detail + kartu "Detail Astronomis" terisi angka yang masuk akal (Az/Alt &
terbit/terbenam butuh izin lokasi granted) + jadwal 4 fase mendatang terisi
tanggal yang masuk akal, tap tombol back → kembali ke `MainActivity`. Sudah
diverifikasi 2026-09-05: ilustrasi sabit render tekstur kawah dengan benar
di kartu home (56dp) & layar detail (160dp), field Detail Astronomis terisi
(Magnitude -9.65, Jarak 369,094 km, dst.), tidak ada exception di logcat.

## 7. Known issues & TODOs

- [ ] Orientasi kiri/kanan limb terang pakai konvensi sederhana (waxing =
      terang di kanan) untuk kejelasan visual, bukan orientasi astronomis
      sungguhan (yang sebenarnya bergantung posisi geografis observer &
      parallactic angle) — cukup untuk ilustrasi info, bukan untuk keperluan
      rukyat presisi.
- [ ] Belum ada test otomatis untuk `MoonPhaseLabel`/logika `MoonPhaseView`.
- [ ] Tekstur `moon_texture.jpg` selalu piringan purnama tanpa libration —
      dipotong ke bentuk sabit/cembung yang benar, tapi corak kawah yang
      kelihatan di tepi limb tidak berubah sesuai libration sungguhan
      tanggal tsb (efek minor, tidak kasat mata pada ukuran tampil 56-160dp).
