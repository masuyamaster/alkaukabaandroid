# Gerhana

## 1. Ringkasan

**Fitur**: Gerhana — daftar 5 gerhana Bulan dan 5 gerhana Matahari terdekat ke
depan dari lokasi markaz (GPS atau setting manual di Konfigurasi, dengan
opsi override khusus layar ini — lihat §2), lengkap dengan waktu lokal,
jenis, magnitude, dan status visibilitas dari lokasi tersebut. Kedua daftar
selalu berisi event **global** ke depan apa adanya (lihat §5) — termasuk
event yang sama sekali tidak terlihat dari markaz yang sedang dipakai,
ditandai badge merah "Tidak terlihat dari lokasimu". Tiap kartu event juga
punya keterangan tambahan "🌍 Terlihat dari: ..." — daftar wilayah makro
dunia (bukan cuma markaz yang sedang dipakai) di mana event itu terlihat,
lihat §5.

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

### Lokasi: override khusus halaman ini (2026-09-17)

Tombol "⟳ Ubah Lokasi" di bar lokasi **tidak lagi** sekadar refresh GPS —
sekarang membuka bottom sheet `dialog_lokasi_halaman.xml` dengan 2 opsi:

1. **"Ikuti pengaturan global"** (default) — perilaku lama: pakai
   `SessionManager` punya lokasi (manual global atau GPS), persis sama
   dengan fitur lain.
2. **"Manual khusus halaman ini"** — lat/lng yang diisi (atau diambil dari
   GPS lewat tombol "Pakai lokasi GPS saat ini" di dalam dialog) disimpan
   ke `SharedPreferences` **terpisah** (`GerhanaPagePrefs`, key
   `PAGE_MANUAL_LAT`/`PAGE_MANUAL_LNG`) — **bukan** ke `SessionManager`.
   Override ini murni lokal untuk `GerhanaActivity`: tidak pernah dibaca
   atau ditulis oleh fitur lain (Waktu Sholat, Kiblat, dll tetap pakai
   lokasi global apa adanya), dan sebaliknya perubahan di Konfigurasi tidak
   menimpa override ini.

`resolveLocationAndCalculate()` mengecek urutan: override halaman
(`hasPageLocationOverride()`) -> lokasi manual global (`SessionManager`) ->
GPS. Override halaman ini persisten antar sesi (SharedPreferences, bukan
in-memory) sampai user memilih ulang "Ikuti pengaturan global" di dialog
yang sama (menghapus key override, bukan menyimpan flag "false").

Diverifikasi manual di emulator (2026-09-17): set override ke Jakarta
(-6.2088, 106.8456) saat lokasi global masih Surabaya -> bar lokasi Gerhana
berubah ke "Kota Jakarta Selatan" dan waktu gerhana Matahari terhitung ulang
sesuai Jakarta, sementara layar Waktu Sholat tetap menampilkan "Surabaya,
Jawa Timur" (lokasi global tidak ikut berubah).

## 3. Alur

1. `onCreate` -> resolve lokasi (manual/GPS/fallback) -> begitu lokasi
   didapat, langsung panggil `GerhanaViewModel.calculateEclipses(lat, lng, 0.0)`
   tanpa perlu tombol ditekan.
2. Perhitungan jalan di `Dispatchers.Default` (bukan main thread) karena
   pencarian gerhana Matahari lokal bisa butuh iterasi beberapa lunasi;
   `isLoading` LiveData mengontrol `ProgressBar`.
3. Hasil dirender ke dua `RecyclerView` (`rvLunarEclipse`, `rvSolarEclipse`)
   yang di-toggle visibility oleh tab "Gerhana Bulan" / "Gerhana Matahari"
   (pola tab sama dengan `WaktuSholatActivity`, tapi warna tab aktif dibuat
   khusus — lihat catatan 2026-08-30 di section 7).
4. `btnRefreshLoc` ("⟳ Ubah Lokasi") membuka dialog pemilihan lokasi (lihat
   §2) -> setelah "Simpan", `resolveLocationAndCalculate()` dipanggil ulang.
5. `btnBack` (toolbar) -> `finish()`.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/gerhana/GerhanaActivity.kt` + `activity_gerhana.xml` | UI: lokasi, tab switch, 2 RecyclerView |
| `res/layout/dialog_lokasi_halaman.xml` | Bottom sheet "Ubah Lokasi" (global vs manual khusus halaman) — lihat §2 |
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
2. **Gerhana Matahari** (diubah 2026-09-17, lihat juga §7): sekarang
   `globalSolarEclipsesAfter(now).take(5)` — **bukan lagi**
   `localSolarEclipsesAfter(now, observer)`. Alasannya: `localSolarEclipsesAfter`
   melompati (skip) event yang sama sekali tidak punya sirkumstansi lokal di
   markaz (mis. seluruh durasi gerhana terjadi saat markaz malam), jadi
   daftar 5 event yang tampil sebelumnya bisa jadi jauh ke depan (skip
   banyak event yang sebetulnya terjadi lebih dulu tapi tidak terlihat dari
   markaz itu). Dengan pencarian global, urutan 5 event yang tampil selalu
   konsisten dengan almanak gerhana global manapun, terlepas dari lokasi
   markaz.

   Untuk tiap event global, `EclipseCalculator.matchLocalCircumstance()`
   mencoba mencari sirkumstansi lokalnya lewat `searchLocalSolarEclipse`
   yang di-seed 3 hari sebelum waktu puncak global, lalu membandingkan waktu
   puncak hasil pencarian itu dengan waktu puncak global (toleransi 3 hari,
   aman karena jarak antar gerhana Matahari beruntun >=29 hari). Dua
   kemungkinan:
   - **Cocok** (event lokal ketemu di bulan baru yang sama) -> pakai rincian
     lengkap dari `LocalSolarEclipseInfo` (Mulai/Puncak/Berakhir/fase
     Total-Cincin/magnitude/`visibleFromLocation` dari `peak.altitude`).
   - **Tidak cocok** (pencarian lokal melompat ke event lain yang lebih
     belakangan, berarti event global ini memang tidak punya sirkumstansi
     lokal apapun di markaz) -> item tetap dibuat, tapi `partialBeginLabel`/
     `partialEndLabel`/`totalBeginLabel`/`totalEndLabel` = `null` dan
     `visibleFromLocation = false`. `magnitudePercent` diisi dari
     `GlobalSolarEclipseInfo.obscuration` untuk jenis Total/Cincin, tapi
     `null` untuk jenis Sebagian (obscuration global tanpa titik observasi
     memang undefined menurut KDoc `GlobalSolarEclipseInfo` di
     `utils/Astronomy.kt`).

   `SolarEclipseAdapter` menyembunyikan baris Mulai/Berakhir/Magnitude
   (`rowPartialBegin`/`rowPartialEnd`/`rowMagnitude` di
   `item_gerhana_matahari.xml`) kalau field terkait `null`, bukan
   menampilkan placeholder "-".
3. **Magnitude**: `obscuration` (0.0–1.0) dari Astronomy Engine, ditampilkan
   sebagai persen (lihat pengecualian gerhana Matahari Sebagian tak-terlihat
   di atas).
4. **Label jenis**: `EclipseKind.Penumbral/Partial/Total` (gerhana Bulan) dan
   `EclipseKind.Partial/Annular/Total` (gerhana Matahari) di-map ke label
   Indonesia "Penumbra"/"Sebagian"/"Total"/"Cincin".
5. **Keterangan "terlihat di mana"** (`visibleRegions`, ditambah 2026-09-17):
   selain badge terlihat/tidak untuk markaz yang sedang dipakai, tiap event
   juga dapat daftar wilayah makro dunia yang bisa melihatnya —
   `EclipseCalculator.VISIBILITY_REGIONS`, 9 titik representatif tetap
   (Indonesia & Asia Tenggara, Asia Timur, Asia Selatan, Timur Tengah,
   Eropa, Afrika, Amerika Utara, Amerika Selatan, Australia & Oseania).
   Untuk tiap event, satu wilayah masuk daftar kalau titik representatifnya
   "terlihat" dengan kriteria **persis sama** dengan `visibleFromLocation`
   markaz (Bulan: `horizon().altitude > 0` saat puncak; Matahari: hasil
   `matchLocalCircumstance()` di titik itu punya `peak.altitude > 0`).

   **Ini aproksimasi kasar, bukan peta jalur gerhana presisi** — beda dari
   `WorldVisibilityCalculator` (fitur Peta Visibilitas Hilal) yang pakai
   grid rapat 15°x15° (~240 titik) dengan kalkulasi penuh per titik; di
   sini cuma 9 titik supaya tetap ringan dihitung untuk 10 event sekaligus
   (~90 pemanggilan tambahan per load layar — untuk gerhana Matahari,
   `matchLocalCircumstance` lewat `searchLocalSolarEclipse` per titik,
   lumayan tapi masih dalam anggaran `Dispatchers.Default` yang sama
   dengan perhitungan utama). Konsekuensinya:
   - Jalur totalitas gerhana Matahari sebenarnya cuma selebar
     puluhan-ratusan km — satu wilayah makro (misal "Amerika Selatan")
     bisa saja cuma sebagian kecil (satu negara/pesisir) yang benar-benar
     dilewati jalur itu, tapi seluruh wilayah tetap ditandai "terlihat"
     kalau titik representatifnya kebetulan berada di jalur/zona parsial.
     Sebaliknya, event yang jalurnya meleset tipis dari titik representatif
     bisa saja tidak masuk daftar padahal ada bagian kecil wilayah itu yang
     sebenarnya melihatnya.
   - Kalau tidak ada satupun dari 9 wilayah yang cocok (umum untuk event
     yang jalurnya cuma lewat kutub/lautan terbuka), `visibleRegions`
     kosong -> UI (`LunarEclipseAdapter`/`SolarEclipseAdapter`) tampilkan
     fallback "🌍 Di luar wilayah acuan (kemungkinan cuma teramati di
     kutub/lautan)" alih-alih baris kosong.
   - Baris ini **selalu ditampilkan** (beda dari baris Mulai/Berakhir/
     Magnitude gerhana Matahari yang disembunyikan kalau `null`) karena
     `visibleRegions` selalu berupa `List<String>` valid (boleh kosong),
     bukan nullable.

## 6. Testing

Tidak ada test otomatis (`app/src/test` masih boilerplate default).
Diverifikasi manual: build + install APK debug, buka "Gerhana" dari home,
cek kedua tab menampilkan 5 kartu dengan tanggal/waktu/magnitude/badge
visibilitas terisi (diverifikasi di emulator Pixel 6 API 34 dengan lokasi
Kabupaten Lamongan — 2026-08-30). Untuk memverifikasi akurasi astronomisnya,
bandingkan tanggal/waktu puncak yang dihasilkan dengan referensi resmi
(mis. publikasi BMKG/NASA eclipse catalog) untuk lokasi & rentang tahun yang
sama.

Per 2026-09-17, diverifikasi manual tambahan di emulator Pixel 4 XL API 36
(lokasi global Surabaya):
- Tab Gerhana Matahari menampilkan 5 event global berurutan (Feb 2027 s.d.
  Jan 2029), termasuk 3 event bertanda "Tidak terlihat dari lokasimu" (baris
  Mulai/Berakhir tersembunyi, magnitude tetap tampil untuk Cincin/Total,
  disembunyikan untuk Sebagian) dan 1 event "Terlihat dari lokasimu" (22 Juli
  2028) dengan rincian Mulai/Puncak/Berakhir/Magnitude lengkap.
- Dialog "Ubah Lokasi" -> pilih "Manual khusus halaman ini" -> isi
  -6.2088/106.8456 -> Simpan: bar lokasi Gerhana berubah ke "Kota Jakarta
  Selatan" dan waktu gerhana Matahari terhitung ulang (mis. event 22 Juli
  2028 berubah dari Mulai 07:46:07/magnitude 92.1% di Surabaya menjadi Mulai
  07:38:04/magnitude 88.7% di Jakarta) — sementara layar Waktu Sholat (fitur
  lain) tetap menampilkan "Surabaya, Jawa Timur", membuktikan override tidak
  bocor ke pengaturan lokasi global.

Per 2026-09-17 (lanjutan sesi yang sama), diverifikasi keterangan
"🌍 Terlihat dari: ..." di kedua tab dengan lokasi override Jakarta Selatan:
tab Gerhana Bulan event 21 Feb 2027 ("Tidak terlihat dari lokasimu") tetap
menampilkan wilayah lain yang terlihat ("Asia Timur, Asia Selatan, Timur
Tengah, Eropa, Afrika, Amerika Selatan" — Indonesia & Amerika Utara
konsisten tidak masuk); tab Gerhana Matahari event 02 Agustus 2027 (Total,
"Tidak terlihat dari lokasimu") menampilkan "Asia Selatan, Timur Tengah,
Eropa, Afrika".

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
- [ ] Override lokasi khusus halaman ini (§2) baru ada di Gerhana — fitur
      astronomi lain yang serupa (Okultasi, Bulan Hijriyah) belum punya pola
      yang sama; kalau mau ditambahkan di sana, tidak disarankan
      langsung generalize `GerhanaPagePrefs` jadi util bersama tanpa diminta
      — tunggu ada kebutuhan konkret di fitur itu dulu.
- [ ] `visibleRegions` (§5) pakai 9 titik representatif tetap, bukan peta
      jalur gerhana presisi — lihat detail konsekuensinya di §5. Kalau nanti
      dibutuhkan akurasi lebih tinggi (mis. nama negara spesifik, bukan
      "wilayah makro"), opsi ke depan: reverse-geocode titik pusat bayangan
      dari `GlobalSolarEclipseInfo.latitude/longitude` (cuma tersedia untuk
      jenis Total/Cincin, `Geocoder` sudah dipakai di `GerhanaActivity` untuk
      nama lokasi markaz) alih-alih grid tetap — belum dikerjakan karena
      tidak berlaku untuk jenis Sebagian & gerhana Bulan.

Per 2026-08-30 (polish UI, belum di-commit): standardisasi visual mengikuti
masukan user —
- Toolbar (`view_toolbar_default.xml`, dipakai bareng oleh layar lain juga:
  Waktu Sholat/Awal Bulan/Konfigurasi/Kiblat) — lingkaran abu-abu di
  belakang `btnBack` dihapus, jadi ikon panah polos dengan ripple borderless.
- Tab aktif ("Gerhana Bulan"/"Gerhana Matahari") pindah dari
  `waktu_sholat_dark_bg` (nyaris hitam, #111827) ke warna baru
  `navy_dongker` (#1E3A5F) lewat drawable baru
  `bg_tab_underline_active_navy.xml` — sengaja dibuat drawable/warna
  terpisah dari `waktu_sholat_dark_bg`, bukan mengubah warna itu langsung,
  supaya tidak ikut mengubah hero card Waktu Sholat yang belum diminta.
- Bar lokasi: emoji pin merah diganti `ic_gis_location_poi` (vector,
  di-tint `navy_dongker`), diberi background abu-abu muda
  (`waktu_sholat_icon_bg_inactive`), dan tombol "Ubah Lokasi" dibesarkan
  jadi tap target 48dp.
- Card item (`item_gerhana_bulan.xml`/`item_gerhana_matahari.xml`):
  tanggal cuma ditampilkan sekali (sebagai sub-heading `navy_dongker`,
  ikon 📅), baris Puncak/Mulai/Berakhir/Fase cuma nampilin jam (adapter
  strip tanggal dari label lewat `substringAfter(", ")` — bergantung pada
  format tetap `EclipseCalculator.formatLocalTime`, "dd MMMM yyyy,
  HH:mm:ss"), badge visibilitas jadi pill berwarna
  (`bg_pill_green`/`bg_pill_red`, reuse drawable yang sama dengan fitur
  Profil) plus ikon 👁️/🚫, dan judul dapet ikon 🌙/☀️.
- Belum diverifikasi di emulator/device fisik (mesin kerja saat ini tidak
  punya JDK/Android Studio terpasang) — build + smoke test manual masih
  perlu dilakukan sebelum commit.
