# Fase Bulan

## 1. Ringkasan

**Fitur**: Fase Bulan — kartu di home screen yang menampilkan ilustrasi 2D
fase bulan saat ini (real-time, berdasarkan waktu sistem device) beserta nama
fase dan persentase permukaan yang tersinari. Tap kartu membuka layar detail
dengan ilustrasi lebih besar dan jadwal 4 fase bulan mendatang (bulan
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
- Prasyarat: tidak ada — tidak butuh lokasi/GPS/permission apa pun, murni
  perhitungan waktu (tanggal & jam device) tanpa panggilan network.

## 3. Titik masuk logika & navigasi

- `MainActivity.setupMoonPhaseCard()` — dipanggil dari `onCreate`, hitung fase
  saat ini dan isi kartu home, pasang `OnClickListener` ke `FaseBulanActivity`.
- `FaseBulanActivity.showCurrentPhase()` — versi lebih besar dari logika yang
  sama di kartu home.
- `FaseBulanActivity.loadUpcomingQuarters()` — cari 4 fase mendatang lewat
  `searchMoonQuarter`/`nextMoonQuarter`, dijalankan di `Dispatchers.Default`
  (iteratif/pencarian akar, dipindah dari main thread) lalu bind ke UI di
  `Dispatchers.Main`.
- `MoonPhaseView` (`ui/widget/MoonPhaseView.kt`) — custom `View` yang reusable,
  dipakai baik di kartu home (56dp) maupun layar detail (160dp) lewat method
  publik `setPhase(phaseAngleDegrees, illuminatedFraction)`.
- `MoonPhaseLabel.forAngle()` (`utils/MoonPhaseLabel.kt`) — mapping sudut fase
  sinodik ke 8 nama fase Bahasa Indonesia.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/widget/MoonPhaseView.kt` | Custom `View`: gambar piringan gelap penuh, lalu area terang dibentuk dari setengah lingkaran (limb terang, kiri/kanan tergantung waxing/waning) dipotong (`Path.Op.DIFFERENCE`, fase sabit) atau ditambah (`Path.Op.UNION`, fase cembung) dengan elips terminator, lebar elips = `r * |1 - 2k|` (`k` = fraksi tersinari). |
| `utils/MoonPhaseLabel.kt` | 8 bucket nama fase (masing-masing 45°) dari sudut sinodik 0-360°. |
| `ui/fasebulan/FaseBulanActivity.kt` + `activity_fase_bulan.xml` | Layar detail: kartu navy besar (ilustrasi + nama fase + %), lalu kartu putih daftar 4 fase mendatang. |
| `MainActivity.kt` | `setupMoonPhaseCard()` mengisi kartu home + wiring klik ke `FaseBulanActivity`. |
| `activity_main.xml` | Kartu `cardMoonPhase`/`btnMoonPhase` (gaya `bg_card_gradient_navy`, konsisten dengan kartu Gerhana). |

Alur data (fase saat ini): `Time.fromMillisecondsSince1970(now)` →
`moonPhase(time)` (sudut sinodik 0-360°, untuk `MoonPhaseView` + label) dan
`illumination(Body.Moon, time).phaseFraction` (fraksi tersinari 0-1, untuk
`MoonPhaseView` + teks persentase) → langsung bind ke UI (sinkron, tidak perlu
coroutine — perhitungan single-point sangat cepat).

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

- Tidak ada tambahan library baru. `Path.op()` (boolean path operations)
  adalah API `android.graphics.Path` bawaan platform (tersedia sejak API 19,
  minSdk repo ini 21) — bukan dependency eksternal.

## 6. Testing

Tidak ada test otomatis. Verifikasi manual dilakukan build debug APK →
install ke emulator `Pixel6_API34` (bukan device fisik, sesuai preferensi
testing project ini) → cek kartu home render ilustrasi + label + persentase
konsisten, tap kartu → cek layar detail + jadwal 4 fase mendatang terisi
tanggal yang masuk akal, tap tombol back → kembali ke `MainActivity`.

## 7. Known issues & TODOs

- [ ] Orientasi kiri/kanan limb terang pakai konvensi sederhana (waxing =
      terang di kanan) untuk kejelasan visual, bukan orientasi astronomis
      sungguhan (yang sebenarnya bergantung posisi geografis observer &
      parallactic angle) — cukup untuk ilustrasi info, bukan untuk keperluan
      rukyat presisi.
- [ ] Belum ada test otomatis untuk `MoonPhaseLabel`/logika `MoonPhaseView`.
