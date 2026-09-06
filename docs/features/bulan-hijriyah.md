# Bulan Hijriyah / Awal Bulan

## 1. Ringkasan

**Fitur**: Bulan Hijriyah / Awal Bulan — hisab awal bulan (ijtima', ghurub,
tinggi hilal mar'i, elongasi, mukuts) untuk bulan Hijriyah mendatang
terdekat, berbasis lokasi markaz (GPS atau setting manual di Konfigurasi),
dengan kesimpulan kriteria Neo-MABIMS dan opsi export hasil ke PDF.

Per 2026-09-06: ada **2 metode hisab**, dipilih via row "Metode Hisab" di
Konfigurasi (`SessionManager.getHisabAwalBulanMethod()`,
`HISAB_AWAL_BULAN_ASTRONOMY_ENGINE` default / `HISAB_AWAL_BULAN_DURRUL_ANIQ`):

1. **Astronomy Engine** (`EphemerisCalculator`, section 5) — ephemeris modern
   `io.github.cosinekitty.astronomy`, sudah ada sejak awal fitur ini.
2. **Ad-Durrul Aniq** (`utils/addurrulaniq/AdDurrulAniqCalculator`, section 5a)
   — kitab hisab klasik Ahmad Ghozali Muhammad Fathulloh, ditambahkan sebagai
   alternatif/pembanding, BUKAN pengganti.

Keduanya menghasilkan `HilalResult` yang sama bentuknya (dipilih di
`HilalViewModel.calculateHilal()`) sehingga UI tidak perlu tahu metode mana
yang aktif — breakdown "Markaz" di kedua metode menyebutkan nama metodenya
supaya user tahu dari hasil yang ditampilkan.

Satu-satunya bagian yang masih pakai pendekatan tabular murni kosmetik
(bukan bagian mesin hisab manapun) adalah label "bulan Hijriyah yang dicek"
di kartu ringkasan (lihat section 4), akurasinya ±1-2 hari — tidak
memengaruhi hasil ijtima'/ghurub/kriteria metode manapun.

## 2. Entry point & navigasi

- Layar: `AwalBulanActivity` (layout `activity_awal_bulan.xml`, judul UI
  "Awal Bulan Hijriyah").
- Terdaftar di `AndroidManifest.xml` dan bisa dibuka dari tombol "Bulan
  Hijriyah" (`bt_awalbulan`) di `MainActivity`/`activity_main.xml`.
- Prasyarat: permission `ACCESS_FINE_LOCATION` (diminta on-the-fly, request
  code `100`, dengan `onRequestPermissionsResult` yang retry GPS otomatis
  setelah izin diberikan). Kalau `SessionManager.isManualLocationMode()`
  aktif (setting lokasi manual di Konfigurasi), GPS/permission dilewati sama
  sekali dan lat/lng manual langsung dipakai. Kalau GPS `null`/permission
  ditolak, fallback ke koordinat hardcode Jakarta (`-6.2088, 106.8456`).
  Semua perhitungan lokal, tidak ada panggilan network.

## 3. Alur

Berbeda dari rencana lama (user pilih tanggal bebas), alurnya sekarang
**otomatis**:

1. `onCreate` -> resolve lokasi (manual/GPS/fallback) -> begitu lokasi
   didapat, langsung panggil `HilalViewModel.calculateHilal(lat, lng,
   heightMeters)` tanpa perlu tombol ditekan dulu.
2. Tombol "Hitung Ulang" (`btnCalculate`) tersedia untuk menghitung ulang
   dengan lokasi/ketinggian saat ini (mis. setelah ubah field ketinggian).
3. `btnRefreshLoc` mengambil ulang lokasi lalu otomatis menghitung ulang.
4. `btnDownloadPdf` -> `HilalViewModel.generatePdf()` -> `HilalPdfService`
   (tidak berubah dari versi lama, cuma baca `HilalResult.calculationLog`).
5. `btnBack` -> `finish()`.

`EphemerisCalculator.calculate()` selalu mencari **ijtima' (new moon)
terdekat ke depan dari waktu sekarang** — tidak ada pilihan bulan manual.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/awalbulan/AwalBulanActivity.kt` + `activity_awal_bulan.xml` | UI: koordinat (auto GPS/manual), ketinggian, tombol Hitung Ulang & Download PDF; kartu ringkasan (label bulan Hijriyah, tanggal ghurub, status badge) + 3 kartu metrik (`item_hilal_result_card.xml`: tinggi hilal mar'i, elongasi, mukuts) + accordion rincian perhitungan |
| `viewmodel/hilal/HilalViewModel.kt` | `LiveData<HilalResult> calculationResult`; jembatan Activity -> `EphemerisCalculator`/`HilalPdfService` |
| `model/HilalModels.kt` | `HilalInput` (lat, lng, heightMeters) dan `HilalResult` (label, status, tinggi hilal, elongasi, mukuts, `breakdownSections`, `calculationLog`) |
| `utils/EphemerisCalculator.kt` | Mesin hisab real — lihat section 5 |
| `utils/HijriDateUtil.kt` | Konversi Masehi->Hijriyah tabular (Kuwaiti algorithm) untuk label tampilan — dipakai untuk "bulan Hijriyah yang dicek" di sini (`nextMonthLabel()`) **dan** tanggal Hijriyah hari ini di kartu Sholat Berikutnya `MainActivity` (`fullDateLabel()`, sejak 2026-08-30) |
| `utils/HilalPdfService.kt` | Render `HilalResult.calculationLog` jadi PDF via `android.graphics.pdf.PdfDocument`, simpan ke folder Download publik — tidak diubah dari versi lama |
| `utils/Astronomy.kt` | "Astronomy Engine" (`io.github.cosinekitty.astronomy`) — sekarang **dipakai** oleh `EphemerisCalculator` (`Observer`, `Time`, `searchMoonQuarter`/`nextMoonQuarter`, `searchRiseSet`, `equator`, `horizon`, `elongation`, `illumination`) |
| `utils/prayerbreakdown/PrayerBreakdownModels.kt` | Model accordion (`PrayerBreakdownSection`/`PrayerBreakdownRow`) — di-reuse dari fitur Waktu Sholat, bukan model baru khusus fitur ini |

Alur data (hitung): lokasi resolved -> `HilalViewModel.calculateHilal()` ->
`HilalInput` -> `EphemerisCalculator.calculate()` -> `HilalResult` -> LiveData
`calculationResult` -> Activity bind ke kartu ringkasan + 3 kartu metrik +
accordion rincian (`ItemPrayerBreakdownBinding` + `item_breakdown_row.xml`,
pola yang sama dengan `WaktuSholatActivity.renderPrayerBreakdown()`).

Per 2026-08-31: `activity_awal_bulan.xml` diubah dari `RelativeLayout`
(tombol `btnCalculate` posisi `alignParentBottom` mengambang **di atas**
`ScrollView`) jadi `LinearLayout` vertikal (`toolbar -> ScrollView weight=1 ->
Button` sebagai sibling biasa) — sebelumnya tombol "Hitung Ulang" menutupi
baris terakhir accordion begitu salah satu section di-expand (tinggi konten
bertambah, padding-bottom tetap statis). Dengan restrukturisasi ini tombol
selalu punya baris sendiri di layout flow, jadi tidak mungkin menutupi konten
apa pun di skenario scroll/expand manapun.

## 5. Mesin hisab (`EphemerisCalculator`)

1. **Ijtima' (konjungsi)**: `searchMoonQuarter`/`nextMoonQuarter` dari waktu
   sekarang sampai ketemu quarter `0` (new moon) berikutnya.
2. **Ghurub markaz**: `searchRiseSet(Body.Sun, ..., Direction.Set, ...)` di
   tengah malam lokal tanggal ijtima'. Kalau ijtima' terjadi setelah ghurub
   hari itu, geser ke ghurub keesokan harinya.
3. **Data Matahari & Bulan saat ghurub**: `equator()` (deklinasi/asensiorekta,
   equator-of-date + aberration corrected) lalu `horizon()` (azimuth/tinggi,
   refraction normal) untuk Matahari dan Bulan. Tinggi topocentric Bulan =
   "tinggi hilal mar'i".
4. **Elongasi**: `elongation(Body.Moon, ghurub)` (geocentric, bukan
   toposentris — pilihan disederhanakan sesuai ticket).
5. **Mukuts**: selisih waktu antara ghurub dan moonset
   (`searchRiseSet(Body.Moon, ...)` setelah ghurub), dalam menit.
6. **Kriteria**: Neo-MABIMS saja (tinggi >= 3°, elongasi >= 6.4°) — tidak ada
   perbandingan kriteria lama, sesuai keputusan di ticket Notion.

Local-date/midnight helper pakai `java.util.Calendar` dengan timezone default
device (bukan hardcode offset WIB seperti versi lama).

## 5a. Mesin hisab (`AdDurrulAniqCalculator`) — metode Ad-Durrul Aniq

Implementasi kitab **"Ad-Durrul Aniq fi Ma'rifatil Hilal wal Kusufain
bit-Tadqiq"** (Ahmad Ghozali Muhammad Fathulloh), ditranskrip dari foto tabel
kitab + rangkuman riset di halaman Notion "Ad-Durul Aniq" (Ruang Perpustakaan
: I am a Reader). Tiga file di `utils/addurrulaniq/`:

| File | Peran |
|---|---|
| `AdDurrulAniqTables.kt` | Tabel Ijtima' (Majmu'ah/Mabsuthah/Bulan, halaman 156-158 kitab) + konstanta Ta'dilul 'Alamah (T1-T8) + konstanta ta'dil Hilal (S1-S2, M1-M9, B1-B4, r1-r4) + tabel konversi Julian->Masehi |
| `AdDurrulAniqIjtimaCalculator.kt` | Hisab Ijtima' (konjungsi) dari tabel di atas + `findNearestFuture()` (cari ijtima' terdekat ke depan dari sekarang, pola sama dgn `EphemerisCalculator`) |
| `AdDurrulAniqHilalCalculator.kt` | Hisab Ghurub + posisi Matahari/Bulan (deklinasi, asensiorekta, azimuth, tinggi, elongasi, illuminasi) |
| `AdDurrulAniqCalculator.kt` | Entry point `calculate(HilalInput): HilalResult` — satukan Ijtima'+Ghurub+Hilal+kriteria, dipanggil `HilalViewModel` |

**Temuan penting selama implementasi** (lihat commit history utk detail):

1. **Ta'dilul 'Alamah (T1-T8)** terlihat seperti tabel lookup 31x6 di kitab,
   tapi terbukti (tervalidasi 2x-silang: contoh Sya'ban & Ramadhan 1434H,
   presisi 4 desimal) murni fungsi `amplitudo x sin(dalil)` — disimpan
   sebagai 8 konstanta (`TadilAlamah`), bukan tabel.
2. **Ta'dil Hilal** (S1-S2 bujur Matahari, M1-M9 bujur Bulan, B1-B4 latitude
   Bulan, r1-r4 jarak Bulan) sama polanya (sin utk S/M/B, **cos** utk r) —
   semua tervalidasi & disimpan sebagai konstanta (`TadilHilal`).
3. **R1/R2** (jarak Bumi-Matahari) SENGAJA tidak dipakai — dampaknya ke
   semidiameter Matahari cuma ~1-2 detik busur (di bawah presisi target
   metode), kitab sendiri sediakan alternatif rumus langsung (Ghurub Wasaty
   cara 2, halaman 12): `sd = 0.267/(1-0.017*cos(m))`.
4. **Kolom "D"/obliquitas** di tabel gerak Hilal ditandai kitab sendiri
   "(tetap)" — dipakai konstan `TadilHilal.OBLIQUITAS = 23.437533`, bukan
   tabel (obliquitas sungguhan berubah ~0.00013°/tahun, tidak relevan).
5. **Terbukti setara ELP2000/Meeus**: rate harian S,m,M,A,N yang diturunkan
   dari data kitab (hari 29 & 30, Sya'ban 1434H) ternyata PERSIS sama dengan
   konstanta gerak rata-rata Matahari/Bulan standar astronomi modern (Meeus,
   *Astronomical Algorithms* bab 22 & 47). Ini juga berlaku utk kedelapan
   T1-T8 (cocok dengan koefisien new-moon standar Meeus bab 49). Karena itu,
   tabel Jadwal Gerak (majmu'ah/mabsuthah/bulan/hari/jam/menit/detik) untuk
   S,m,M,A,N — yang jadi hambatan transkrip terbesar (10 kolom padat, rawan
   salah baca foto) — **diganti rumus polynomial langsung dari Julian Day**
   (`AdDurrulAniqHilalCalculator.dalilDariJulianDay()`), tervalidasi <0.002
   derajat terhadap dalil tabel kitab.
6. **Sidereal time** (kolom "O" di kitab) diganti rumus GMST standar Meeus
   dari Julian Day (`gmstDerajat()`) — lebih presisi & berlaku universal,
   tidak terikat konvensi markaz Sampang seperti tabel aslinya.
7. **Formula azimuth kitab** (`tan⁻¹` gabungan) cuma beri hasil mentah
   -90..90 (rentang atan); utk konteks ghurub/moonset azimuth sejati selalu
   ada di belahan barat (180-360°), jadi perlu pergeseran **+270** (bukan
   quadrant generik +360-jika-negatif) — lihat komentar
   `azimuthDariSudutWaktu()`.

**Yang MASIH pakai tabel kitab (bukan rumus)**: hisab Ijtima' (Majmu'ah
tahun -180..1770/Mabsuthah 1-30/Bulan 1-12 utk Alamat/Hishshatul-Ardh/
Khashshah/Markaz) — berbeda dari S,m,M,A,N Hilal, kuantitas ini punya
kompounding rounding antar-baris yang tidak bisa direproduksi rumus rate
sederhana (lihat komentar `sumDalil()`), jadi harus tabel asli. Satu baris
(`tahunMajmuah[1020]`) ditandai TODO — F dan M' terbaca sama persis saat
transkrip, kemungkinan salah baca foto, belum memengaruhi 4 contoh
tervalidasi tapi perlu verifikasi ke buku fisik kalau ada kebutuhan hitung
Hijriyah tahun ~1021-1050.

**Validasi**: `AdDurrulAniqIjtimaCalculatorTest` (4/4 contoh manual kitab —
Sya'ban 1434H, Ramadhan 1434H, Shafar 1434H/Vancouver, Shafar -52H/Makkah),
`AdDurrulAniqHilalCalculatorTest` (Sya'ban 1434H — deklinasi, azimuth,
tinggi, elongasi Matahari & Bulan, semua presisi tinggi), `AdDurrulAniqCalculatorTest`
(uji integrasi end-to-end utk tanggal sekarang — bukan cocok-persis ke buku
karena tidak ada contoh kitab utk tanggal sembarang, yang dicek kewajaran
fisis: ijtima' & ghurub di hari yang sama, kriteria konsisten dgn angka).

**Verifikasi manual di emulator (Pixel6_API34, 2026-09-06)**: toggle metode
di Konfigurasi berhasil ganti hasil hitung `AwalBulanActivity` tanpa restart
app, setting persist setelah app di-restart. Dibandingkan langsung kedua
metode utk tanggal & lokasi sama (Jakarta Selatan, ijtima' 11 September 2026):

| | Astronomy Engine | Ad-Durrul Aniq | Selisih |
|---|---|---|---|
| Ijtima' | 10:27:28 | 10:25:15 | ~2 menit |
| Ghurub | 17:50:40 | 17:50:42 | 2 detik |
| Tinggi Hilal | 1.69° | 1.32° | 0.37° |
| Elongasi | 4.42° | 3.81° | 0.61° |
| Status kriteria | Belum Memenuhi | Belum Memenuhi | konsisten |

Dua implementasi yang sepenuhnya independen (library astronomi modern vs
rumus kitab klasik) saling cocok dalam orde menit/derajat kecil — validasi
silang tambahan di luar unit test, utk 1 tanggal/lokasi nyata (bukan
perbandingan sistematis banyak tanggal, lihat Known limitations).

## 6. Testing

`EphemerisCalculator` tidak ada test otomatis (`app/src/androidTest` masih
boilerplate default) — verifikasi manual: install APK debug, buka "Bulan
Hijriyah" dari home, cek kartu ringkasan/status/accordion terisi, dan export
PDF menghasilkan file di Download. Untuk memverifikasi akurasi astronomisnya,
bandingkan ijtima'/ghurub yang dihasilkan dengan referensi resmi (mis. jadwal
Kemenag atau publikasi PCNU/Al-Kaukaba Lamongan) untuk bulan yang sama.

`AdDurrulAniqCalculator` (dan Ijtima'/Hilal calculator-nya) PUNYA test JVM
otomatis di `app/src/test/.../utils/addurrulaniq/` (lihat section 5a) — jalankan
`./gradlew testDebugUnitTest --tests "*.addurrulaniq.*"`.

## 6a. Visualisasi Wujud Hilal (bantuan rukyah)

Per 2026-09-05: ditambahkan kartu "Wujud Hilal saat Ghurub" di
`activity_awal_bulan.xml`, antara kartu ringkasan dan 3 kartu metrik. Reuse
`MoonPhaseView` (widget yang sama dengan kartu "Fase Bulan" di home) lewat
`setWaxingCrescent(illumFraction)` (hilal awal bulan selalu waxing crescent
karena selalu tak lama setelah ijtima') + `setBrightLimbAngle(tiltDegrees)`
untuk memutar ilustrasi sesuai kemiringan limb terang sungguhan di langit.

`tiltDegrees` dihitung oleh `utils/MoonTilt.brightLimbAngleDegrees()` dari
azimuth/altitude Matahari & Bulan saat ghurub (`sunHor`/`moonHor`, sudah
dihitung `EphemerisCalculator` untuk section "Data Matahari/Bulan saat
Ghurub" — sekarang juga diekspos lewat `HilalResult.azimuthHilal` /
`azimuthMatahari` / `tinggiMatahari` / `illumFraction`). Metodenya: proyeksi
vektor arah Matahari ke bidang tangen langit di posisi Bulan (basis "atas" =
komponen zenith tegak lurus arah pandang ke Bulan, "kanan" tegak lurus
keduanya) — tervalidasi terhadap kasus "hilal senyum" khatulistiwa (azimuth
Matahari≈Bulan, Matahari jauh di bawah ufuk relatif Bulan → limb terang lurus
ke bawah, horns menghadap atas).

Ilustrasi punya lantai tampilan minimum 5% (`MoonPhaseView`,
`coerceAtLeast(0.05)`) karena hilal nyata di ambang kriteria Neo-MABIMS bisa
<0.1% tersinari — kalau digambar apa adanya, lebar sabitnya sub-piksel dan
tidak kelihatan sama sekali. Persentase asli tetap ditampilkan sebagai teks
terpisah; kartu juga diberi disclaimer "Ilustrasi kemiringan, bukan skala
sebenarnya".

Sekalian, `MoonPhaseView` diganti dari pendekatan `Path.op`
(union/difference boolean) ke konstruksi dua-arc langsung, karena `Path.op`
terbukti tidak stabil ketika elips terminator hampir sekoinsiden dengan
lingkaran luar (persis kasus hilal sangat tipis) — sempat membuat sabit tidak
tergambar sama sekali di percobaan pertama.

## 6b. Fix label breakdown row hilang untuk value teks panjang

Per 2026-09-06, ditemukan saat verifikasi manual toggle metode Ad-Durrul Aniq
di emulator: `item_breakdown_row.xml` (dipakai accordion di sini **dan** di
Waktu Sholat) sebelumnya kasih `tvRowLabel` weight=1 (fleksibel) + `tvRowValue`
wrap_content — aman selama value pendek (mis. koordinat/derajat), tapi baris
baru "Metode" (isinya "Ad-Durrul Aniq (Ahmad Ghozali Muhammad Fathulloh)",
jauh lebih panjang dari value lain) bikin label terdesak nyaris 0 lebar dan
tidak kelihatan sama sekali.

Diperbaiki dengan menukar alokasi: `tvRowLabel` jadi `wrap_content` (selalu
tampil penuh), `tvRowValue` yang dapat `layout_weight="1"` + `gravity="end"`
(bisa wrap ke baris berikutnya, rata kanan). Diverifikasi ulang di emulator —
tidak mengubah tampilan baris pendek yang sudah ada (Lintang/Bujur/Ketinggian
di sini, Lintang/Bujur/Deklinasi/dst di Waktu Sholat).

## 7. Known limitations

- [ ] Label "bulan Hijriyah yang dicek" pakai kalender tabular (Kuwaiti
      algorithm, `HijriDateUtil`), akurasi ±1-2 hari — murni kosmetik, tidak
      memengaruhi hasil hisab.
- [ ] Elongasi yang dipakai untuk kriteria adalah geocentric
      (`elongation()`), bukan toposentris — simplifikasi yang disengaja
      sesuai ticket, tapi perlu dicatat kalau nanti ada kebutuhan presisi
      lebih tinggi.
- [ ] Belum ada test otomatis untuk `EphemerisCalculator`/`HijriDateUtil`
      (lihat section 6).
- [ ] Orientasi kemiringan hilal (`MoonTilt`) diturunkan & divalidasi lewat
      penalaran vektor + satu kasus fisik yang dikenal (hilal senyum
      khatulistiwa), bukan dibandingkan langsung ke foto rukyah sungguhan —
      kalau suatu saat ada laporan orientasi kelihatan terbalik/miring salah
      di lapangan, mulai cek dari sini (`utils/MoonTilt.kt`).
- [ ] (Ad-Durrul Aniq) Tabel Ijtima' `tahunMajmuah[1020]` — F dan M' terbaca
      identik saat transkrip foto, kemungkinan salah baca, belum
      diverifikasi ke buku fisik (lihat section 5a).
- [ ] (Ad-Durrul Aniq) Tabel `JulianMasehiTables` baru mencakup tahun Masehi
      400-2900 — cukup utk seluruh rentang tabel Ijtima' (-180..1770 H) tapi
      kalau kitab diperluas ke tahun Masehi < 400, perlu tabel majmu'ah
      miladiyah tambahan.
- [ ] (Ad-Durrul Aniq) Belum ada perbandingan sistematis hasil vs
      `EphemerisCalculator` utk banyak tanggal/lokasi berbeda — baru
      tervalidasi ketat terhadap 1 contoh kitab (Sya'ban 1434H) + uji
      kewajaran fisis utk tanggal sekarang.
