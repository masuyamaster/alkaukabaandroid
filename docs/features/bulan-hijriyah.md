# Bulan Hijriyah / Awal Bulan

## 1. Ringkasan

**Fitur**: Bulan Hijriyah / Awal Bulan — hisab awal bulan (ijtima', ghurub,
tinggi hilal) berbasis lokasi & tanggal yang dipilih user, dengan opsi export
hasil ke PDF.

Tujuannya menampilkan hasil hisab awal bulan Hijriyah (Ijtima'/konjungsi
bulan, saat matahari terbenam, tinggi hilal haqiqi) ala format buku Ephemeris
Hisab Rukyat, lalu bisa diunduh sebagai laporan PDF. **Catatan penting**: saat
ini mesin hitungnya masih data simulasi (lihat section 4 & 7) dan Activity-nya
tidak terhubung ke navigasi manapun di app — lihat detail di bawah sebelum
menganggap fitur ini "jalan" di produksi.

## 2. Entry point & prasyarat

- Layar: `AwalBulanActivity` (layout `activity_awal_bulan.xml`, judul UI
  "Awal Bulan Hijriyah").
- **Tidak ada satu pun caller** yang menavigasi ke Activity ini. Sudah digrep
  di seluruh `app/src/main` (kode Kotlin & XML) — tidak ada `Intent(...,
  AwalBulanActivity::class.java)` di `MainActivity.kt` atau file lain mana
  pun. Tombol-tombol di `MainActivity` (Waktu Sholat, Kiblat, Kalender,
  Konfigurasi) semuanya mengarah ke Activity lain, tidak ada yang ke sini.
- Lebih parah lagi: `AwalBulanActivity` **tidak didaftarkan di
  `AndroidManifest.xml`** sama sekali (dibandingkan `WaktuSholatActivity`,
  `KiblatActivity`, `CalendarActivity`, `KonfigurasiActivity` yang semuanya
  punya entry `<activity>`). Kalau ada kode yang mencoba
  `startActivity(Intent(this, AwalBulanActivity::class.java))` hari ini, app
  akan crash `ActivityNotFoundException` saat runtime. Lihat section 7.
- Prasyarat: permission `ACCESS_FINE_LOCATION` (diminta on-the-fly lewat
  `ActivityCompat.requestPermissions`, request code `100`, tidak ada
  penanganan hasil permission — lihat section 7). Kalau GPS `null`/permission
  ditolak, fallback ke koordinat hardcode Jakarta (`-6.2088, 106.8456`).
  Tidak ada API key/config server khusus — semua perhitungan lokal, tidak ada
  panggilan network.

## 3. Titik masuk logika & navigasi

- `HilalViewModel.calculateHilal(lat, lng, alt, ref, date)` — dipanggil dari
  tombol "Hitung" (`btnCalculate`), parse `alt`/`ref` dari `EditText` (fallback
  `10.0` m dan `0.034` kalau kosong/tidak valid), bungkus jadi `HilalInput`,
  lalu delegasikan ke `EphemerisCalculator.calculate()`. Hasil `HilalResult`
  di-set ke `LiveData calculationResult`.
- `HilalViewModel.generatePdf(context)` — dipanggil dari tombol download PDF
  (`btnDownloadPdf`), ambil `calculationResult.value` (kalau belum pernah
  hitung, `null` dan tidak melakukan apa-apa) lalu delegasikan ke
  `HilalPdfService.generatePdf()`.
- `EphemerisCalculator.calculate(input: HilalInput): HilalResult` — satu-
  satunya titik "mesin hisab". Kalau nanti mesin hisab real mau
  diimplementasikan, di sinilah tempatnya, lihat section 7.
- Navigasi: **tidak ada** — Activity ini tidak punya Activity tujuan lain
  yang di-`startActivity()` dari dalamnya (tombol `btnBack` di layout ada,
  tapi lihat section 7: tidak ada `setOnClickListener` untuk itu di kode),
  dan tidak ada Activity lain yang menavigasi ke sini (lihat section 2).

## 4. Struktur & alur data

File yang terlibat:

| File | Peran |
|---|---|
| `ui/awalbulan/AwalBulanActivity.kt` + `activity_awal_bulan.xml` | UI: input koordinat (auto dari GPS), altitude, refraksi, date picker, tombol Hitung & Download PDF; bind `HilalResult` ke 3 card (`item_hilal_result_card.xml` via `cardIjtima`/`cardGhurub`/`cardHilalHeight`) |
| `viewmodel/hilal/HilalViewModel.kt` | `LiveData<HilalResult> calculationResult`; jembatan Activity → `EphemerisCalculator`/`HilalPdfService`, tanpa logic tambahan |
| `model/HilalModels.kt` | `HilalInput` (lat, lng, altitude, refraction, date) dan `HilalResult` (ijtimaTime, ghurubTime, moonAltitude, moonElongation, calculationLog) — pure data class |
| `utils/EphemerisCalculator.kt` | "Mesin hisab" — lihat penjelasan simulasi di bawah |
| `utils/HilalPdfService.kt` | Render `HilalResult.calculationLog` jadi file PDF pakai `android.graphics.pdf.PdfDocument` (API Android native, bukan library eksternal), simpan ke folder Download publik |
| `utils/Astronomy.kt` | Library pihak ketiga "Astronomy Engine" (`io.github.cosinekitty.astronomy`, MIT license, dari `github.com/cosinekitty/astronomy`) yang di-vendor penuh ke repo — **tidak dipakai/di-import di mana pun** saat ini (sudah digrep, satu-satunya match adalah file itu sendiri) |

Alur data (hitung): `AwalBulanActivity` (`btnCalculate` click, baca
`etAltitude`/`etRefraksi`/`selectedDate`/`currentLat`/`currentLng`) →
`HilalViewModel.calculateHilal()` → `HilalInput` → `EphemerisCalculator
.calculate()` → `HilalResult` → LiveData `calculationResult` → Activity
`setupObservers()` bind ke 3 card hasil (Ijtima', Ghurub, Tinggi Hilal).

Alur data (PDF): `AwalBulanActivity` (`btnDownloadPdf` click) →
`HilalViewModel.generatePdf()` (ambil `calculationResult.value` terakhir) →
`HilalPdfService.generatePdf()` → tulis `calculationLog` baris-per-baris ke
`PdfDocument` A4 → simpan `Hisab_Hilal_<timestamp>.pdf` di
`Environment.DIRECTORY_DOWNLOADS`.

### Status mesin hisab: SIMULASI, bukan perhitungan astronomi real

Sudah diverifikasi langsung dari kode `EphemerisCalculator.calculate()`
(bukan cuma percaya komentar), dan benar 100% simulasi:

- Semua angka input astronomis di-hardcode sebagai konstanta di dalam
  fungsi: `fib = 18.0`, `elm = 175.954`, `alb = 175.528`, `sb = 0.600`,
  `sm = 0.040` — komentar di kode sendiri bilang "Contoh Data dari
  screenshot 'Ijtima''" dan "Dalam aplikasi nyata, nilai ini diambil dari
  Library Swiss Ephemeris atau Algoritma Jean Meeus" (belum diimplementasikan
  di sini).
- `ghurubWIB = 17.80` juga hardcode, dengan komentar eksplisit
  `// Jam 17:48 (Simulasi)`.
- `moonAltitude = "04° 37' 53\""` dan `moonElongation = "04° 53' 03\""` di
  `HilalResult` yang dikembalikan adalah string literal tetap, dengan
  komentar `// Data dummy sesuai screenshot`.
- **Input `HilalInput` (`latitude`, `longitude`, `altitude`, `refraction`)
  sama sekali tidak dipakai dalam perhitungan** — cuma `input.date` yang
  dipakai, itu pun hanya untuk `formatDate()` di teks log PDF, bukan sebagai
  variabel rumus. Artinya ganti koordinat/altitude/refraksi/tanggal tidak
  mengubah hasil ijtima'/ghurub/tinggi hilal sama sekali — hasil kalkulasi
  identik untuk input apapun.
- `Astronomy.kt` (library asli "Astronomy Engine" yang bisa hitung posisi
  Matahari/Bulan sungguhan) sudah ada di repo tapi **tidak diimport atau
  dipanggil dari `EphemerisCalculator.kt` maupun file manapun** — jadi
  library real-nya sudah "nangkring" di repo tapi belum disambungkan.

Ini konsisten dengan yang disebut di `docs/features/waktu-sholat.md`
("`EphemerisCalculator.kt` yang sudah ada sekarang berisi data simulasi
untuk fitur Awal Bulan Hijriyah") — klaim itu terverifikasi benar.

## 5. Dependencies & tech stack khusus

- `android.graphics.pdf.PdfDocument` — API PDF bawaan Android SDK (bukan
  library pihak ketiga seperti iText/PDFBox), dipakai di `HilalPdfService`.
- `com.google.android.gms:play-services-location` (`FusedLocationProviderClient`)
  — sudah dipakai juga di `WaktuSholatActivity`, `MainActivity`, dan
  `KiblatActivity`, jadi bukan tambahan khusus untuk fitur ini.
- `utils/Astronomy.kt` — library "Astronomy Engine" (`io.github.cosinekitty
  .astronomy`) sudah di-vendor di repo sebagai satu file besar (~437 KB),
  tapi berdiri sendiri, tidak terhubung ke fitur ini atau fitur lain mana
  pun saat ini (lihat section 4).

## 6. Testing

Tidak ada test otomatis untuk fitur ini. Dicek: `app/src/test` dan
`app/src/androidTest` cuma berisi boilerplate default
(`ExampleUnitTest.kt`/`ExampleInstrumentedTest.kt`), tidak ada test untuk
`EphemerisCalculator`, `HilalViewModel`, `HilalPdfService`, atau
`AwalBulanActivity`.

Verifikasi manual saat ini **tidak bisa dilakukan lewat UI app** karena
Activity tidak ter-reach dari navigasi manapun dan tidak terdaftar di
manifest (lihat section 2 & 7). Untuk mengecek fitur ini secara manual,
developer perlu salah satu dari:

1. Tambah sementara `<activity>` entry untuk `AwalBulanActivity` di
   `AndroidManifest.xml` dan tombol/`Intent` pemicu dari `MainActivity`
   (atau Activity lain) untuk bisa membukanya di emulator, ATAU
2. Launch langsung via `adb shell am start -n
   site.elahady.alkaukaba/Site.elahady.alkaukaba.ui.awalbulan.AwalBulanActivity`
   — tapi ini akan tetap gagal selama Activity belum didaftarkan di manifest.

Karena hasil hisabnya simulasi (angka hardcode, tidak dipengaruhi input),
verifikasi manual pun tidak banyak gunanya untuk mengecek kebenaran hisab —
paling hanya untuk mengecek UI/PDF export berfungsi secara mekanis.

## 7. Known issues & TODOs

- [ ] **Mesin hisab masih 100% simulasi/dummy**, bukan perhitungan astronomi
      real — lihat section 4. Semua angka (`ijtimaTime`, `ghurubTime`,
      `moonAltitude`, `moonElongation`) hardcode dan tidak berubah walau
      input (lokasi/tanggal/altitude/refraksi) diganti.
- [ ] **`AwalBulanActivity` tidak terdaftar di `AndroidManifest.xml`** —
      berbeda dari semua Activity lain di app ini (`WaktuSholatActivity`,
      `KiblatActivity`, `CalendarActivity`, `KonfigurasiActivity` semuanya
      punya entry `<activity>`). Selama belum ditambahkan, Activity ini
      tidak bisa dibuka sama sekali (crash `ActivityNotFoundException` kalau
      dipaksa via `Intent`/`adb`).
- [ ] **Tidak ada satu pun layar yang menavigasi ke `AwalBulanActivity`** —
      fitur ini "dead-end", tidak terhubung dari `MainActivity` atau layar
      manapun. Kombinasi dengan poin di atas artinya fitur ini secara efektif
      tidak bisa diakses user sama sekali di kondisi kode saat ini.
- [ ] `utils/Astronomy.kt` — library astronomi real ("Astronomy Engine",
      MIT license, dari `cosinekitty/astronomy`) sudah di-vendor penuh di
      repo tapi belum disambungkan ke `EphemerisCalculator` atau kode
      manapun. Kemungkinan ini persiapan untuk mengganti mesin hisab
      simulasi jadi real, tapi belum ada progres pemanggilannya — cek ulang
      sebelum asumsi ini "siap pakai", karena API publik library ini juga
      belum diverifikasi cocok/tidaknya untuk kebutuhan hisab awal bulan di
      sini.
- [ ] `btnBack` dan `btnRefreshLoc` ada di `activity_awal_bulan.xml` tapi
      **tidak punya `setOnClickListener` di `AwalBulanActivity.kt`** — kedua
      tombol itu tidak melakukan apa-apa kalau ditekan.
- [ ] `getLocation()` memanggil `ActivityCompat.requestPermissions(...,
      100)` tapi **tidak ada override `onRequestPermissionsResult`** di
      `AwalBulanActivity` — kalau user baru memberi izin lewat dialog
      permission, app tidak otomatis retry ambil lokasi; harus keluar-masuk
      layar lagi (yang sekarang pun tidak bisa karena poin dead-end di atas).
- [ ] Tidak ada test otomatis sama sekali untuk fitur ini (lihat section 6).
- [ ] Ada entry `<!-- <activity android:name=".DetailPerhitunganActivity" ...
      -->` yang di-comment-out di `AndroidManifest.xml` — kemungkinan sisa
      rencana Activity detail perhitungan yang belum/batal dibangun, tidak
      terkait langsung ke `AwalBulanActivity` tapi disebutkan di sini karena
      ditemukan saat verifikasi manifest untuk dokumen ini.
