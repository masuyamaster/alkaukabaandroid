# Gerhana

## 1. Ringkasan

**Fitur**: Gerhana — daftar 5 gerhana Bulan dan 5 gerhana Matahari terdekat ke
depan dari lokasi markaz (GPS atau setting manual di Konfigurasi), lengkap
dengan waktu lokal, jenis, magnitude, dan status visibilitas dari lokasi
tersebut.

Perhitungan 100% oleh "Astronomy Engine" (`utils/Astronomy.kt`,
`io.github.cosinekitty.astronomy`, sama seperti fitur Bulan Hijriyah) —
tidak ada panggilan network.

## 2. Entry point & prasyarat

- Layar: `GerhanaActivity` (layout `activity_gerhana.xml`, judul UI
  "Gerhana").
- Terdaftar di `AndroidManifest.xml`, dibuka dari tombol "Gerhana"
  (`bt_gerhana`) di `MainActivity`/`activity_main.xml`.
- Prasyarat: sama seperti Bulan Hijriyah — permission `ACCESS_FINE_LOCATION`
  diminta on-the-fly (request code `100`); kalau `SessionManager.isManualLocationMode()`
  aktif, GPS/permission dilewati dan lat/lng manual langsung dipakai; kalau
  GPS `null`/permission ditolak, fallback ke koordinat Jakarta
  (`-6.2088, 106.8456`). Ketinggian markaz tidak diinput manual di layar ini
  (selalu 0 m — tidak berpengaruh signifikan terhadap hasil gerhana).

## 3. Alur

1. `onCreate` -> resolve lokasi (manual/GPS/fallback) -> begitu lokasi
   didapat, langsung panggil `GerhanaViewModel.calculateEclipses(lat, lng, 0.0)`
   tanpa perlu tombol ditekan.
2. Perhitungan jalan di `Dispatchers.Default` (bukan main thread) karena
   pencarian gerhana Matahari lokal bisa butuh iterasi beberapa lunasi;
   `isLoading` LiveData mengontrol `ProgressBar`.
3. Hasil dirender ke dua `RecyclerView` (`rvLunarEclipse`, `rvSolarEclipse`)
   yang di-toggle visibility oleh tab "Gerhana Bulan" / "Gerhana Matahari"
   (pola tab sama dengan `WaktuSholatActivity` — `bg_tab_underline_active`/`inactive`).
4. `btnRefreshLoc` mengambil ulang lokasi lalu otomatis menghitung ulang.
5. `btnBack` (toolbar) -> `finish()`.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/gerhana/GerhanaActivity.kt` + `activity_gerhana.xml` | UI: lokasi, tab switch, 2 RecyclerView |
| `viewmodel/gerhana/GerhanaViewModel.kt` | `LiveData<GerhanaResult> result` + `LiveData<Boolean> isLoading`; jembatan Activity -> `EclipseCalculator` (dijalankan di `Dispatchers.Default` via `viewModelScope`) |
| `model/GerhanaModels.kt` | `LunarEclipseItem`, `SolarEclipseItem` (model tampilan siap-render), `GerhanaResult` (bungkus keduanya) |
| `utils/EclipseCalculator.kt` | Mesin hisab — lihat section 5 |
| `adapter/LunarEclipseAdapter.kt`, `adapter/SolarEclipseAdapter.kt` | Adapter `RecyclerView` masing-masing jenis gerhana, layout item `item_gerhana_bulan.xml`/`item_gerhana_matahari.xml` (pola sama dengan `HolidayAdapter`/`item_holiday.xml`) |

Alur data: lokasi resolved -> `GerhanaViewModel.calculateEclipses()` ->
`EclipseCalculator.calculate()` -> `GerhanaResult` -> LiveData `result` ->
Activity bind ke `lunarAdapter`/`solarAdapter`.

## 5. Mesin hisab (`EclipseCalculator`)

1. **Gerhana Bulan**: `lunarEclipsesAfter(now).take(5)` dari Astronomy Engine
   — pencarian ini **global** (bukan per-lokasi), karena gerhana Bulan pada
   dasarnya terlihat dari mana pun di belahan Bumi yang malam saat itu.
   Visibilitas lokal dihitung manual per item: `equator()` + `horizon()`
   Bulan pada waktu `peak`, `visibleFromLocation = altitude > 0`.
2. **Gerhana Matahari**: `localSolarEclipsesAfter(now, observer).take(5)` —
   Astronomy Engine sudah punya varian pencarian per-lokasi untuk ini
   (`searchLocalSolarEclipse`), jadi hasilnya otomatis relevan untuk markaz
   yang dipakai. **Catatan penting**: hasil `searchLocalSolarEclipse` bisa
   saja punya `peak.altitude` negatif (gerhana terjadi tapi Matahari di
   bawah ufuk saat puncak, cuma sebagian fase yang kelihatan saat
   terbit/tenggelam) — makanya `visibleFromLocation` tetap dihitung eksplisit
   dari `info.peak.altitude > 0.0`, bukan diasumsikan selalu `true`.
3. **Magnitude**: `obscuration` (0.0–1.0) dari Astronomy Engine, ditampilkan
   sebagai persen.
4. **Label jenis**: `EclipseKind.Penumbral/Partial/Total` (gerhana Bulan) dan
   `EclipseKind.Partial/Annular/Total` (gerhana Matahari) di-map ke label
   Indonesia "Penumbra"/"Sebagian"/"Total"/"Cincin".

## 6. Testing

Tidak ada test otomatis (`app/src/test` masih boilerplate default).
Diverifikasi manual: build + install APK debug, buka "Gerhana" dari home,
cek kedua tab menampilkan 5 kartu dengan tanggal/waktu/magnitude/badge
visibilitas terisi (diverifikasi di emulator Pixel 6 API 34 dengan lokasi
Kabupaten Lamongan — 2026-08-30). Untuk memverifikasi akurasi astronomisnya,
bandingkan tanggal/waktu puncak yang dihasilkan dengan referensi resmi
(mis. publikasi BMKG/NASA eclipse catalog) untuk lokasi & rentang tahun yang
sama.

## 7. Known limitations

- [ ] Belum ada test otomatis untuk `EclipseCalculator`.
- [ ] Ketinggian markaz selalu 0 m (tidak ada input manual di layar ini,
      beda dari Bulan Hijriyah yang punya field ketinggian) — pengaruhnya ke
      hasil gerhana dianggap dapat diabaikan.
- [ ] Untuk gerhana Matahari yang partial-visible (matahari terbit/tenggelam
      di tengah fase), badge visibilitas hanya mengecek altitude di titik
      puncak (`peak.altitude`), bukan mengecek apakah *sebagian* fase
      (mis. cuma awal atau cuma akhir) tetap kelihatan — simplifikasi yang
      disengaja, konsisten dengan pola badge boolean tunggal di gerhana
      Bulan.
