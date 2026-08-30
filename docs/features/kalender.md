# Kalender & Hari Besar Islam

## 1. Ringkasan

**Fitur**: Kalender — widget kalender bulanan (grid tanggal Masehi + Hijriah)
di beranda, plus daftar Hari Besar Islam mendatang (preview di beranda dan
halaman "lihat semua" berupa list yang bisa dicari/difilter).

Fitur ini memberi user gambaran tanggal Hijriah hari ini/bulan berjalan tanpa
buka app kalender terpisah, sekaligus mengingatkan hari besar Islam yang akan
datang (mis. Isra Mi'raj, Maulid Nabi) supaya user tidak kelewatan.

## 2. Entry point & prasyarat

- Widget kalender bulanan (grid 7 kolom) dan preview hari besar (list 3 item
  teratas) tampil langsung di `MainActivity` (beranda), dipicu otomatis saat
  lokasi user didapat (lihat `fetchDataByCoordinate()` yang memanggil
  `viewModel.initCalendar(lat, lon)` dan
  `viewModel.fetchUpcomingIslamicHolidays(lat, lon)`).
- Tombol prev/next bulan (`btnPrevMonth`/`btnNextMonth`) di widget beranda
  memanggil `viewModel.changeMonth(-1/+1)` — hanya mengubah grid kalender,
  tidak mempengaruhi preview hari besar (preview selalu "bulan berjalan saat
  app dibuka", tidak ikut navigasi prev/next).
- Toast alert hari besar hari ini (`viewModel.holidayAlert`, di-observe di
  `MainActivity`) muncul otomatis kalau `fetchPrayerData()` (dipanggil dari
  `fetchDataByCoordinate()`) mendeteksi hari ini ada di daftar hari libur
  Hijriah dari response Aladhan, atau (fallback) ada di map hardcode
  `checkNationalHoliday()` (cuma 2 entri: 17 Agustus & 1 Januari).
- Halaman "lihat semua hari besar" (`CalendarActivity`) dibuka dari 3 tempat
  di `MainActivity`: `tvLabelCalendar`, `tvLabelDetailCalendar` (lewat
  `openCalendarPage()`), dan `btnSeeAllHolidays` (lewat listener terpisah di
  `setupHolidayPreview()`) — lihat section 3 untuk perbedaan extra yang
  dibawa masing-masing.
- Prasyarat: permission lokasi (`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`,
  sudah diminta di alur umum `MainActivity`) — dipakai untuk parameter
  `lat`/`lng` ke Aladhan API karena posisi Hijriah/metode hisab dipengaruhi
  koordinat. Tidak ada API key khusus (memakai base URL Aladhan yang sama
  dengan fitur Waktu Sholat, lihat `RetrofitClient`).

## 3. Titik masuk logika & navigasi

- `MainViewModel.initCalendar(lat, lng)` / `changeMonth(amount)` /
  `fetchMonthlyCalendar()` (private) — mengisi grid kalender bulanan
  (`calendarData`, `hijriTitle`, `monthYearTitle`) di widget beranda.
- `MainViewModel.fetchUpcomingIslamicHolidays(lat, lng)` — mengisi preview
  hari besar (`holidayPreview`, 3 item teratas, bulan berjalan saja) di
  beranda.
- `PrayerRepository.getIslamicHolidays(lat, lng, month, year)` — **satu-satunya**
  jalur data untuk grid kalender maupun preview/list hari besar (lihat
  penjelasan section 4). Method ini murni memanggil `api.getCalendar(...)` ke
  Aladhan dengan `method`/`methodSettings` yang sama seperti fitur Waktu
  Sholat (baca `docs/features/waktu-sholat.md` untuk konteks preset metode).
- `CalendarActivity.fetchYearlyHolidays(lat, lng)` — logika terpisah (bukan
  lewat `MainViewModel`) yang memanggil `repository.getIslamicHolidays()`
  berulang untuk tiap bulan dari bulan berjalan sampai Desember tahun
  berjalan, lalu digabung jadi satu list hari besar mendatang. Ini titik
  colok kalau mau ubah rentang tahun (mis. lanjut ke tahun depan juga).
- Navigasi:
  - `MainActivity.tvLabelCalendar` / `tvLabelDetailCalendar` →
    `openCalendarPage()` → `Intent` ke `CalendarActivity` **dengan** extra
    `LATITUDE`/`LONGITUDE` (double, dari `MainActivity.latitude/longitude`
    yang sudah terisi dari lokasi user).
  - `MainActivity.btnSeeAllHolidays` (di `setupHolidayPreview()`) → `Intent`
    ke `CalendarActivity` **tanpa** extra apa pun — lihat known issue di
    section 7.
  - `CalendarActivity.btnBack` → `finish()` (kembali ke `MainActivity`).

## 4. Struktur & alur data

### Kalender vs Hari Besar: satu sumber data, dua tampilan

Ini bagian paling gampang bikin bingung: **grid kalender bulanan** (widget di
beranda) dan **daftar Hari Besar Islam** (preview beranda + halaman
`CalendarActivity`) **sama-sama berasal dari satu endpoint yang sama**,
`PrayerRepository.getIslamicHolidays()` → Aladhan `api.getCalendar(...)`
(bukan `HolidayApiService`/`HolidayApi` — lihat catatan dead code di bawah).
Endpoint ini mengembalikan data harian sebulan penuh (`CalendarResponse`),
tiap hari punya info Masehi + Hijriah + daftar nama hari libur Hijriah kalau
ada. Dua ViewModel/Activity yang berbeda mengolah response yang sama ini
dengan cara berbeda:

- `MainViewModel.fetchMonthlyCalendar()` mengubah tiap hari jadi `DayUIModel`
  (dipakai `CalendarAdapter`, grid 7 kolom) — termasuk hari tanpa hari besar
  (`isHoliday = false`, cuma tampil titik/dot kalau `isHoliday = true`).
- `MainViewModel.fetchUpcomingIslamicHolidays()` **memfilter** hanya hari
  yang `hijri.holidays` tidak kosong, ambil 3 teratas ke depan → jadi
  `HolidayItem` (dipakai `HolidayAdapter`, list card).
- `CalendarActivity.fetchYearlyHolidays()` melakukan filter yang sama
  (`hijri.holidays` tidak kosong) tapi memanggil `getIslamicHolidays()`
  berulang untuk semua bulan sisa tahun berjalan, tanpa batas jumlah item,
  plus fitur cari (`etSearch`) dan filter rentang tanggal
  (`btnStartDate`/`btnEndDate`).

### `CalendarActivity` = halaman list "lihat semua", bukan grid bulan penuh

`CalendarActivity` **tidak** merender grid kalender apa pun (tidak ada
`CalendarAdapter`/`GridLayoutManager` di file ini maupun di
`activity_calendar.xml`) — judulnya "Kalender & Hari Besar" tapi isinya murni
`RecyclerView` list (`HolidayAdapter`) berisi hari besar mendatang, dengan
search box + filter tanggal. Grid bulanan (visual kalender 7 kolom) **hanya**
ada di widget beranda `MainActivity` (`rvWeeklyCalendar` + `CalendarAdapter`),
tidak ada di `CalendarActivity`.

### Dead code: `HolidayApiService.kt` tidak dipakai

`api/HolidayApiService.kt` (interface `HolidayApi`, object
`HolidayRetrofitClient`, base URL `https://api-harilibur.vercel.app/` — API
hari libur nasional Indonesia pihak ketiga) **tidak direferensikan di mana
pun** selain di filenya sendiri. Baik grid kalender maupun daftar hari besar
sama sekali tidak memakai endpoint ini — keduanya lewat Aladhan via
`PrayerRepository.getIslamicHolidays()`. Class `HolidayItem` yang benar-benar
dipakai (`CalendarAdapter`, `HolidayAdapter`, `MainViewModel`,
`CalendarActivity`) juga **didefinisikan di file yang sama**
(`HolidayApiService.kt`), jadi jangan salah kira: `HolidayItem` dipakai luas,
tapi `HolidayApi`/`HolidayRetrofitClient` di file yang sama tidak.

File yang terlibat:

| File | Peran |
|---|---|
| `MainActivity.kt` | Host widget kalender bulanan (`rvWeeklyCalendar`) & preview hari besar (`rvHolidayPreview`) di beranda; trigger `initCalendar`/`fetchUpcomingIslamicHolidays` saat lokasi didapat; navigasi ke `CalendarActivity` |
| `viewmodel/MainViewModel.kt` | State beranda: `calendarData`/`hijriTitle`/`monthYearTitle` (grid), `holidayPreview` (list 3 item), `holidayAlert` (toast hari ini); logic `fetchMonthlyCalendar()` & `fetchUpcomingIslamicHolidays()` |
| `repo/PrayerRepository.kt` (`getIslamicHolidays`) | Jembatan tunggal ke Aladhan `api.getCalendar(lat, lng, method, month, year, methodSettings)` — dipakai oleh grid kalender, preview hari besar, dan `CalendarActivity` |
| `adapter/CalendarAdapter.kt` | Adapter grid 7 kolom (`DayUIModel`: tanggal Masehi, tanggal Hijriah, flag hari ini/hari besar/slot kosong) — dipakai di `MainActivity` saja |
| `adapter/HolidayAdapter.kt` | Adapter list card hari besar (`HolidayItem`: tanggal, tanggal Hijriah, keterangan) — dipakai di `MainActivity` (preview) dan `CalendarActivity` (list lengkap) |
| `ui/calendar/CalendarActivity.kt` | Halaman "lihat semua hari besar": ambil data bulan berjalan s.d. Desember, search + filter rentang tanggal |
| `api/HolidayApiService.kt` | Berisi `HolidayItem` (data class, dipakai luas) **dan** `HolidayApi`/`HolidayRetrofitClient` (dead code, tidak dipakai — lihat di atas) |
| `res/layout/activity_calendar.xml` | Layout `CalendarActivity`: header, search box, date range filter, `RecyclerView` list — tanpa grid |

Alur data (grid kalender beranda): `MainActivity.fetchDataByCoordinate()` →
`viewModel.initCalendar(lat, lng)` → `fetchMonthlyCalendar()` →
`PrayerRepository.getIslamicHolidays()` → Aladhan `getCalendar` → diproses
jadi `List<DayUIModel>` (isi 1 bulan penuh + slot kosong padding awal
minggu) → LiveData `calendarData` → `MainActivity` bind ke `CalendarAdapter`
lewat `GridLayoutManager(7)`. Navigasi bulan (`changeMonth`) mengulang alur
yang sama dengan `currentCalendar` yang sudah digeser.

Alur data (preview hari besar beranda): `fetchDataByCoordinate()` →
`viewModel.fetchUpcomingIslamicHolidays(lat, lng)` → panggil
`getIslamicHolidays()` untuk bulan berjalan → filter hari yang punya
`hijri.holidays`, filter tanggal >= hari ini, urutkan, `take(3)` →
`List<HolidayItem>` → LiveData `holidayPreview` → `MainActivity` bind ke
`HolidayAdapter` di `rvHolidayPreview`.

Alur data (halaman lihat semua): `CalendarActivity.onCreate()` → baca extra
`LATITUDE`/`LONGITUDE` dari `Intent` (default Jakarta kalau tidak ada) →
`fetchYearlyHolidays()` → loop `getIslamicHolidays()` per bulan (bulan
berjalan s.d. Desember tahun berjalan) → gabung semua hasil, filter tanggal
>= hari ini, urutkan → `originalList` → `HolidayAdapter` di `rvHolidays`.
Search (`etSearch`) dan filter tanggal (`btnStartDate`/`btnEndDate`) bekerja
di atas `originalList` yang sudah ada di memori (`applyFilter()`), tidak
memanggil API lagi.

## 5. Dependencies & tech stack khusus

Tidak ada tambahan khusus di luar stack umum app — Retrofit + Gson (Aladhan
API, sama dengan fitur Waktu Sholat), `RecyclerView`/`GridLayoutManager` dari
AndroidX, dan `DatePickerDialog` bawaan Android untuk filter rentang tanggal
di `CalendarActivity`.

## 6. Testing

Belum ada test otomatis untuk fitur ini — project cuma punya boilerplate
`ExampleUnitTest.kt` (`app/src/test`) dan `ExampleInstrumentedTest.kt`
(`app/src/androidTest`), tidak ada test nyata untuk kalender/hari besar sama
sekali. Verifikasi saat ini manual:

1. Build & install debug APK (lihat `CLAUDE.md` root untuk perintah
   `gradlew`).
2. Buka `MainActivity`, izinkan lokasi → pastikan grid kalender beranda
   terisi (tanggal Masehi + Hijriah per hari, hari ini ditandai, titik pada
   hari yang ada hari besar) dan judul bulan Masehi/Hijriah (`tvMonth`,
   `tvYear`, `tvHijriMonthYear`) sesuai.
3. Tap `btnPrevMonth`/`btnNextMonth` → pastikan grid & judul berubah, tanpa
   mempengaruhi list preview hari besar di bawahnya.
4. Pastikan preview hari besar (`rvHolidayPreview`) menampilkan maksimal 3
   item hari besar Islam terdekat bulan berjalan (kosong kalau memang tidak
   ada hari besar bulan ini — cek log `error :: ...` di Logcat untuk kasus
   ini, bukan crash).
5. Tap `tvLabelCalendar`/`tvLabelDetailCalendar` **dan** `btnSeeAllHolidays`
   secara terpisah → buka `CalendarActivity` dari kedua jalur, bandingkan
   apakah daftar yang tampil konsisten (lihat known issue lat/lng di bawah;
   perbedaan mungkin baru kelihatan kalau lokasi user jauh dari Jakarta).
6. Di `CalendarActivity`: coba search nama hari besar, filter rentang
   tanggal, dan reset filter → pastikan list ter-update sesuai tanpa network
   call baru (semua di memori).

## 7. Known issues & TODOs

- [ ] **Extra `LATITUDE`/`LONGITUDE` hilang di jalur `btnSeeAllHolidays`.**
      `MainActivity.setupHolidayPreview()` membuka `CalendarActivity` lewat
      `Intent` polos tanpa extra lat/lng (baris ~239-242), sedangkan jalur
      `tvLabelCalendar`/`tvLabelDetailCalendar` via `openCalendarPage()`
      selalu membawa keduanya. `CalendarActivity` sendiri punya fallback
      (`intent.getDoubleExtra("LATITUDE", -6.2088)` / default Jakarta), jadi
      tidak crash — tapi kalau lokasi user jauh dari Jakarta, data hari besar
      yang tampil dari tombol "lihat semua" di preview bisa beda (secara
      astronomis, tergantung metode hisab) dari yang dilihat lewat label
      kalender. Perbaikan: sertakan extra lat/lng juga di listener
      `btnSeeAllHolidays`, idealnya pakai helper `openCalendarPage()` yang
      sudah ada supaya tidak duplikasi.
- [ ] **`api/HolidayApiService.kt` berisi dead code.** `HolidayApi` +
      `HolidayRetrofitClient` (API pihak ketiga `api-harilibur.vercel.app`
      untuk hari libur nasional Indonesia) tidak dipakai di mana pun — semua
      alur kalender/hari besar sudah lewat Aladhan via
      `PrayerRepository.getIslamicHolidays()`. Kemungkinan sisa eksperimen
      sebelum pindah ke sumber data Aladhan. Perlu diputuskan: hapus, atau
      pakai beneran untuk melengkapi hari libur nasional non-Islam (`17-08`,
      `01-01`) yang saat ini masih hardcode 2 entri di
      `MainViewModel.checkNationalHoliday()`.
- [ ] **`checkNationalHoliday()` cuma hardcode 2 hari libur nasional**
      (Kemerdekaan RI, Tahun Baru Masehi) sebagai fallback toast hari besar
      kalau hari ini bukan hari besar Hijriah. Tidak mencakup hari libur
      nasional lain (Natal, Waisak, dll) — kalau mau lengkap, ini titik yang
      relevan untuk hubungkan ke `HolidayApiService` (lihat poin di atas) atau
      sumber data lain.
- [ ] **`CalendarActivity` hanya mengambil hari besar s.d. Desember tahun
      berjalan** (`fetchYearlyHolidays()`, loop `currentMonth..12`). Kalau
      dibuka di bulan Desember dan sudah tidak ada hari besar tersisa tahun
      itu, list akan kosong (toast "Tidak ada data") — tidak otomatis lanjut
      ke tahun berikutnya.
- [ ] Preview hari besar (`holidayPreview`) hanya mengambil bulan berjalan
      (tidak loop ke bulan berikutnya seperti `CalendarActivity`), jadi kalau
      bulan ini kebetulan tidak ada hari besar Islam, preview di beranda
      kosong meskipun bulan depan ada — beda perilaku dari halaman "lihat
      semua" yang mencari sampai akhir tahun.
- [ ] Belum ada test otomatis (lihat bagian Testing di atas).
