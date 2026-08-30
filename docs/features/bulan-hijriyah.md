# Bulan Hijriyah / Awal Bulan

## 1. Ringkasan

**Fitur**: Bulan Hijriyah / Awal Bulan — hisab awal bulan (ijtima', ghurub,
tinggi hilal mar'i, elongasi, mukuts) untuk bulan Hijriyah mendatang
terdekat, berbasis lokasi markaz (GPS atau setting manual di Konfigurasi),
dengan kesimpulan kriteria Neo-MABIMS dan opsi export hasil ke PDF.

Perhitungan dilakukan 100% oleh "Astronomy Engine" (`utils/Astronomy.kt`,
`io.github.cosinekitty.astronomy`, MIT license, vendored penuh di repo) —
bukan lagi simulasi/hardcode seperti versi sebelumnya. Satu-satunya bagian
yang masih pakai pendekatan tabular (bukan Astronomy Engine) adalah label
"bulan Hijriyah yang dicek" di kartu ringkasan (lihat section 4), yang
akurasinya ±1-2 hari — itu murni kosmetik dan tidak memengaruhi hasil
ijtima'/ghurub/kriteria.

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

## 6. Testing

Tidak ada test otomatis untuk fitur ini (`app/src/test` dan
`app/src/androidTest` masih boilerplate default). Verifikasi sejauh ini
manual: install APK debug, buka "Bulan Hijriyah" dari home, cek kartu
ringkasan/status/accordion terisi, dan export PDF menghasilkan file di
Download. Untuk memverifikasi akurasi astronomisnya, bandingkan
ijtima'/ghurub yang dihasilkan dengan referensi resmi (mis. jadwal Kemenag
atau publikasi PCNU/Al-Kaukaba Lamongan) untuk bulan yang sama.

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
