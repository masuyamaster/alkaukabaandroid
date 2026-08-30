# Waktu Sholat — Metode Perhitungan

## 1. Ringkasan

**Fitur**: Waktu Sholat — konfigurasi metode perhitungan.

User bisa memilih metode perhitungan waktu sholat sendiri lewat halaman
Konfigurasi, alih-alih dikunci ke satu metode hardcode seperti sebelumnya.
Ini menyelesaikan kebutuhan bahwa metode hisab waktu sholat itu banyak
macamnya (Kemenag RI, MWL, ISNA, dll) dan preferensinya beda-beda per user.

## 2. Entry point & prasyarat

- Dari `MainActivity`: tap ikon gear (kanan atas) → langsung membuka
  `KonfigurasiActivity` (tidak lewat dialog perantara).
- Di `KonfigurasiActivity`, tap baris **"Metode Perhitungan"** → membuka
  bottom sheet `dialog_prayer_method.xml` berisi daftar preset.
- Prasyarat: tidak ada permission/API key tambahan — semua preset memakai
  base URL Aladhan API yang sudah dikonfigurasi di `RetrofitClient`.

## 3. Titik masuk logika & navigasi

- `PrayerCalculationMethods.PRESETS` — daftar preset yang ditampilkan di UI.
  Tambah/ubah/hapus preset dilakukan di sini.
- `SessionManager.setPrayerMethodId()` / `getPrayerMethodId()` — baca/tulis
  pilihan user.
- `PrayerRepository.getPrayerTimes/getIslamicHolidays/getTimingPrayers` —
  satu-satunya jalur yang benar-benar memanggil Aladhan API; semua sudah
  otomatis memakai method yang tersimpan di `SessionManager`, jadi pemanggil
  (ViewModel) tidak perlu tahu soal method sama sekali.
- `PrayerTimesViewModel.buildSchedule()` — business logic "sholat mana yang
  aktif sekarang" (dari `TimingPrayers` mentah jadi `PrayerScheduleUiState`
  siap-render). Sengaja di ViewModel, bukan `WaktuSholatActivity`, supaya
  Activity cuma bind data ke View (lihat catatan clean-code di bawah).
- `PrayerCalculationBreakdownRegistry.providerFor(methodId)` — titik ekstensi
  untuk breakdown detail perhitungan ("kenapa hasilnya segini") per metode.
  Untuk menambah breakdown ke metode lain: implement interface
  `PrayerCalculationBreakdownProvider`, daftarkan di map registry ini —
  ViewModel/Activity tidak perlu diubah.
- Navigasi: `MainActivity` → `KonfigurasiActivity` via `Intent` biasa (tidak
  ada extra yang dibawa). Dari dalam `KonfigurasiActivity`, bottom sheet metode
  dan konfirmasi logout keduanya modal (`BottomSheetDialog` / `AlertDialog`),
  bukan Activity terpisah.

## 4. Struktur & alur data

File yang terlibat:

| File | Peran |
|---|---|
| `utils/PrayerCalculationMethods.kt` | Data class + daftar preset (id, nama, subtitle) |
| `utils/SessionManager.kt` | Persistensi pilihan user di `SharedPreferences` |
| `repo/PrayerRepository.kt` | Jembatan ke Aladhan API, resolve method yang benar dikirim |
| `api/PrayersApiService.kt` (`AladhanApi`) | Interface Retrofit ke `api.aladhan.com`, terima param `method` & `methodSettings` |
| `viewmodel/waktusholat/PrayerTimesViewModel.kt` | State untuk layar `WaktuSholatActivity`, panggil `PrayerRepository` |
| `viewmodel/MainViewModel.kt` | State untuk widget waktu sholat di home, juga lewat `PrayerRepository` |
| `ui/konfigurasi/KonfigurasiActivity.kt` + `activity_konfigurasi.xml`, `dialog_prayer_method.xml` | UI pemilihan metode & logout |
| `ui/waktusholat/WaktuSholatActivity.kt` | Murni bind `PrayerScheduleUiState` ke View (icon, warna, teks) — tidak ada logika "mana yang aktif" di sini |
| `utils/prayerbreakdown/PrayerCalculationBreakdownRegistry.kt` | Map `methodId -> PrayerCalculationBreakdownProvider`; hari ini isinya cuma Ephemeris |
| `utils/prayerbreakdown/PrayerBreakdownModels.kt` | `PrayerBreakdownSection`/`PrayerBreakdownRow` (data) + interface `PrayerCalculationBreakdownProvider` |
| `utils/prayerbreakdown/EphemerisPrayerCalculator.kt` | Implementasi breakdown untuk Ephemeris — deklinasi matahari & Equation of Time pakai pendekatan sinusoidal sederhana, bukan tabel ephemeris presisi tinggi |
| `res/layout/item_prayer_breakdown.xml` | Card accordion per waktu sholat (header klik untuk expand/collapse + body berisi baris rumus) |
| `res/layout/item_breakdown_row.xml` | Satu baris rumus di dalam card (`tvRowLabel`/`tvRowValue`) |

Alur data (pilih metode): `KonfigurasiActivity` (radio pilih preset) →
`SessionManager` (simpan id) → `PrayerRepository` (baca id saat request
berikutnya) → `AladhanApi` (kirim `method`/`methodSettings`) → Aladhan API.

Alur data (render jadwal di `WaktuSholatActivity`): `PrayerRepository`
(`TimingPrayers` mentah) → `PrayerTimesViewModel.buildSchedule()` (hitung
`activeIndex`, susun `PrayerScheduleItem` per baris) → `PrayerScheduleUiState`
lewat LiveData `prayerSchedule` → Activity `updateNextPrayerUI()` cuma
mem-bind ke `binding.row*`, tanpa logika bisnis apa pun.

Alur data (breakdown perhitungan): `PrayerTimesViewModel.loadData()` panggil
`calculatePrayerBreakdown()` → `PrayerCalculationBreakdownRegistry.providerFor(
repository.getSelectedMethodId())` (pakai id method yang **dipilih user**,
bukan hasil fallback Aladhan) → kalau ada provider, `EphemerisPrayerCalculator
.breakdown()` dipanggil dan hasilnya (`List<PrayerBreakdownSection>?`) di-set
ke LiveData `calculationBreakdown`. Null/kosong kalau metode aktif tidak punya
breakdown terdaftar (semua metode Aladhan selain Ephemeris) → Activity
`renderPrayerBreakdown()` sembunyikan `layoutPrayerBreakdownContainer` dan
tampilkan pesan fallback `tvNoPrayerBreakdown` untuk kasus ini.

UI-nya sendiri: `WaktuSholatActivity` punya dua tab, **"Waktu Aktual"**
(`btnTabActual`, jadwal biasa — default aktif) dan **"Detail Perhitungan"**
(`btnTabDetail`, breakdown ini), diswitch lewat `updateTabState()` yang
toggle visibility `layoutWaktuSholat` vs `layoutDetailPerhitungan`. Tab ini
murni tentang waktu sholat — sebelumnya sempat tercampur dengan detail rumus
Arah Kiblat (`tvCalculationResult`/`tvResultDegree`/`QiblaCalculator`) di
layout yang sama (`layoutDetailKiblat`, nama lama), yang keliru karena Arah
Kiblat itu fitur terpisah (`KiblatActivity`) — sudah dibersihkan, lihat bagian
"Kenapa Arah Kiblat dikeluarkan" di bawah. Setiap section
breakdown (per waktu sholat) dirender sebagai card accordion
(`item_prayer_breakdown.xml`, klik `rowHeader` expand/collapse `layoutBody`,
`tvChevron` berubah ⌄/⌃) berisi baris-baris rumus (`item_breakdown_row.xml`,
`tvRowLabel`/`tvRowValue`) dari `PrayerBreakdownSection.rows`.

### Preset yang tersedia

| Preset | id (dikirim ke Aladhan sbg `method`) | Catatan |
|---|---|---|
| Ephemeris (Al Hasib - Alkaukaba Team) | `1000` (id internal, lihat bagian di bawah) | Belum ada mesin hisab manual |
| Muslim World League | `3` | |
| Islamic Society of North America (ISNA) | `2` | |
| Umm al-Qura, Makkah | `4` | |
| Egyptian General Authority | `5` | |
| University of Islamic Sciences, Karachi | `1` | |
| Custom | `99` | pakai `methodSettings`, lihat `SessionManager.getMethodSettingsQuery()` |

Preferensi tersimpan di `SharedPreferences` (`"AppSession"`):
- `PRAYER_METHOD_ID` — default: `PrayerCalculationMethods.EPHEMERIS_ID`
  (fresh install langsung memakai Ephemeris sebagai metode yang ditampilkan).
- `PRAYER_CUSTOM_FAJR` / `PRAYER_CUSTOM_ISHA` — sudut custom, dipakai saat
  method = Custom (`99`).

### Kenapa "Ephemeris" fallback ke Kemenag RI

Al-Kaukaba ingin identitas metode sendiri di app ("Ephemeris — Al Hasib"),
tapi mesin hitungnya (posisi bulan/matahari, semacam algoritma Jean
Meeus/Swiss Ephemeris) belum diimplementasikan di app ini.
`utils/EphemerisCalculator.kt` yang sudah ada sekarang berisi data simulasi
untuk fitur Awal Bulan Hijriyah, bukan untuk Waktu Sholat, dan tidak dipakai
di sini.

Sampai mesin hisab manual itu siap, memilih **"Ephemeris" di UI tetap secara
teknis mengirim `method=20` (Kemenag RI) ke Aladhan API** di belakang layar —
lihat konstanta `EPHEMERIS_FALLBACK_ALADHAN_METHOD_ID` di
`PrayerRepository.kt`.

Ini keputusan sadar, bukan bug:
- Label **"Ephemeris (Al Hasib - Alkaukaba Team)"** di UI tidak boleh berubah
  jadi "Kemenag RI" di manapun (termasuk subtitle) — dikonfirmasi eksplisit.
- Tapi secara teknis harus tetap mengirim id `method` yang valid ke Aladhan,
  karena `1000` bukan id yang dikenali Aladhan dan akan gagal kalau dikirim
  langsung.

### Kenapa Arah Kiblat dikeluarkan dari layar ini

Sebelumnya, tab "Detail Perhitungan" di `WaktuSholatActivity` menampilkan DUA
hal sekaligus: "1. PERHITUNGAN ARAH KIBLAT" (pakai `QiblaCalculator`, hasil
`detailFormulaSteps` + derajat) dan "2. DETAIL PERHITUNGAN WAKTU SHOLAT"
(breakdown Ephemeris). Ini keliru secara scope — Arah Kiblat adalah fitur
sendiri dengan layarnya sendiri (`ui/arahkiblat/KiblatActivity`), tidak ada
hubungannya dengan Waktu Sholat selain kebetulan sama-sama butuh lat/long.

Sudah dihapus dari sini: `QiblaCalculator` import, `calculateQibla()`,
LiveData `qiblaDetailText`/`qiblaDegreeUI` di `PrayerTimesViewModel`, dan
View terkait (`etCoordinates`, `tvCalculationResult`, `tvResultDegree`,
"Koordinat Pengguna") di `activity_waktu_sholat.xml`. `layoutDetailKiblat`
di-rename jadi `layoutDetailPerhitungan` supaya nama file/id tidak lagi
menyesatkan (isinya sekarang murni waktu sholat).

**Konsekuensi**: `utils/QiblaCalculator.kt` sekarang jadi kelas yatim (tidak
dipakai di manapun) — `KiblatActivity` punya mekanisme sendiri
(`KiblatViewModel.fetchQiblaAngle`) yang tidak memakai kelas ini sama sekali.
File belum dihapus (masih berisi breakdown rumus arah kiblat yang mungkin
berguna), tapi perlu keputusan: hapus, atau sambungkan ke `KiblatActivity`
kalau breakdown rumus semacam ini juga diinginkan di sana. Lihat Known Issues.

## 5. Dependencies & tech stack khusus

Tidak ada tambahan khusus di luar stack umum app — Retrofit + Gson (untuk
panggil Aladhan API) dan `SharedPreferences` bawaan Android, keduanya sudah
dipakai di bagian lain app ini juga.

## 6. Testing

Belum ada test otomatis untuk fitur ini (project baru punya boilerplate
`ExampleUnitTest.kt`/`ExampleInstrumentedTest.kt`, belum ada test nyata sama
sekali). Verifikasi saat ini manual:

1. Build & install debug APK (lihat `CLAUDE.md` root untuk perintah `gradlew`).
2. Buka `MainActivity` → tap gear → pastikan langsung ke `KonfigurasiActivity`
   (bukan dialog perantara).
3. Tap "Metode Perhitungan" → pastikan semua preset tampil dengan label yang
   benar, radio Custom memunculkan input sudut Fajr/Isya.
4. Pilih preset lain → Simpan → pastikan label di baris "Metode Perhitungan"
   berubah sesuai pilihan.
5. (Opsional, kalau mengubah `PrayerRepository`) Cek lewat `adb logcat` atau
   proxy bahwa request ke Aladhan benar-benar mengirim `method` yang sesuai.
6. Buka `WaktuSholatActivity` → tab "Detail Perhitungan" → pastikan isinya
   **cuma** breakdown waktu sholat (tidak ada lagi "PERHITUNGAN ARAH KIBLAT").

## 7. Known issues & TODOs

- [ ] Mesin hisab manual Ephemeris (Al Hasib) — belum diimplementasikan.
      Kemungkinan menyusul setelah fitur Awal Bulan Hijriyah (yang juga masih
      simulasi di `EphemerisCalculator.kt`) beres duluan. Kalau sudah siap:
      1. Ganti isi `currentMethod()` di `PrayerRepository.kt` — untuk
         `PrayerCalculationMethods.EPHEMERIS_ID`, panggil mesin hisab lokal
         alih-alih fallback ke Aladhan.
      2. Kemungkinan perlu ubah tipe return `getPrayerTimes`/`getTimingPrayers`
         di repository supaya bisa datang dari dua sumber (API vs hasil hitung
         lokal) — saat ini keduanya diasumsikan selalu berasal dari Aladhan
         (`Response<PrayerResponse>` / `Response<PrayerTimeResponse>`).
- [ ] Belum ada test otomatis (lihat bagian Testing di atas).
- [ ] Tab "Detail Perhitungan" cuma berguna untuk metode **Ephemeris** — untuk
      semua metode Aladhan lain, tab ini hanya menampilkan pesan fallback
      (`tvNoPrayerBreakdown`) karena memang tidak ada breakdown terdaftar di
      `PrayerCalculationBreakdownRegistry` untuk method selain Ephemeris.
      Kalau mau breakdown juga tersedia untuk metode lain, perlu provider
      baru per metode (lihat titik ekstensi di section 3).
- [ ] `utils/QiblaCalculator.kt` sekarang yatim piatu (tidak dipakai di
      manapun) setelah dikeluarkan dari layar ini — lihat "Kenapa Arah Kiblat
      dikeluarkan". Perlu keputusan: hapus, atau sambungkan ke `KiblatActivity`
      kalau breakdown rumus arah kiblat memang diinginkan di sana.
