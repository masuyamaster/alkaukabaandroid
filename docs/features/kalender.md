# Kalender & Event Besar

## 1. Ringkasan

**Fitur**: Kalender — widget kalender bulanan (grid tanggal Masehi + Hijriah)
di beranda, plus daftar **Event Besar** mendatang (preview "Event Besar
Segera" di beranda dan halaman "lihat semua" berupa list yang bisa
dicari/difilter). "Event Besar" = hari besar Islam + hari libur nasional +
**fenomena astronomi** (Hari Tanpa Bayangan, ekuinoks/solstis, Purnama Panen,
oposisi planet, puncak hujan meteor, gerhana) dalam satu daftar, dibedakan
lewat filter jenis (Semua / Hari Besar / Astronomi).

Fitur ini memberi user gambaran tanggal Hijriah hari ini/bulan berjalan tanpa
buka app kalender terpisah, sekaligus mengingatkan hari besar Islam yang akan
datang (mis. Isra Mi'raj, Maulid Nabi) dan fenomena falak yang bisa diamati
(mis. hari saat Matahari tepat di atas kepala) supaya user tidak kelewatan.
Nama fitur sebelumnya "Kalender & Hari Besar Islam"; nama file dokumen ini
tetap `kalender.md`.

## 2. Entry point & prasyarat

- Widget kalender bulanan (grid 7 kolom) dan preview hari besar (list 3 item
  teratas) tampil langsung di `MainActivity` (beranda), dipicu otomatis saat
  lokasi user didapat (lihat `fetchDataByCoordinate()` yang memanggil
  `viewModel.initCalendar(lat, lon)` dan
  `viewModel.fetchUpcomingEvents(lat, lon)`).
- Tombol prev/next bulan (`btnPrevMonth`/`btnNextMonth`) di widget beranda
  memanggil `viewModel.changeMonth(-1/+1)` — hanya mengubah grid kalender,
  tidak mempengaruhi preview hari besar (preview selalu "bulan berjalan saat
  app dibuka", tidak ikut navigasi prev/next).
- Toast alert hari besar hari ini (`viewModel.holidayAlert`, di-observe di
  `MainActivity`) muncul otomatis kalau `fetchPrayerData()` (dipanggil dari
  `fetchDataByCoordinate()`) mendeteksi hari ini ada di daftar hari libur
  Hijriah dari response Aladhan, atau (fallback) ada di map hardcode
  `checkNationalHoliday()` (cuma 2 entri: 17 Agustus & 1 Januari).
- Halaman "lihat semua event besar" (`CalendarActivity`) dibuka dari 3 tempat
  di `MainActivity`: `tvLabelCalendar`, `tvLabelDetailCalendar`, dan
  `btnSeeAllHolidays` — ketiganya lewat `openCalendarPage()` sehingga selalu
  membawa extra `LATITUDE`/`LONGITUDE` (penting: Hari Tanpa Bayangan dihitung
  per koordinat).
- Prasyarat: permission lokasi (`ACCESS_FINE_LOCATION`/`ACCESS_COARSE_LOCATION`,
  sudah diminta di alur umum `MainActivity`) — dipakai untuk parameter
  `lat`/`lng` ke Aladhan API karena posisi Hijriah/metode hisab dipengaruhi
  koordinat. Tidak ada API key khusus (memakai base URL Aladhan yang sama
  dengan fitur Waktu Sholat, lihat `RetrofitClient`).

## 3. Titik masuk logika & navigasi

- `MainViewModel.initCalendar(lat, lng)` / `changeMonth(amount)` /
  `fetchMonthlyCalendar()` (private) — mengisi grid kalender bulanan
  (`calendarData`, `hijriTitle`, `monthYearTitle`) di widget beranda.
- `MainViewModel.fetchUpcomingEvents(lat, lng)` — mengisi preview "Event Besar
  Segera" (`holidayPreview`, 3 item teratas) di beranda: hari besar Islam
  bulan berjalan + bulan depan (Aladhan) digabung dengan fenomena astronomi
  60 hari ke depan (`UPCOMING_ASTRONOMY_DAYS`), diurut menurut tanggal.
  Sebelumnya bernama `fetchUpcomingIslamicHolidays` dan hanya bulan berjalan.
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
  Event astronomi ditambahkan dari `AstronomicalEventCalculator` untuk
  rentang yang sama (hari ini s.d. 31 Desember), dan filter jenis
  (`selectedJenis`, chip Semua/Hari Besar/Astronomi) ikut diterapkan di
  `applyFilter()`.
- `utils/AstronomicalEventCalculator.calculate(lat, lng, height, startMillis,
  endMillis, zone)` — hisab lokal semua fenomena astronomi dalam rentang;
  dipanggil dari `MainViewModel.fetchUpcomingEvents()` dan
  `CalendarActivity.fetchYearlyHolidays()`. Ekstensi
  `AstronomicalEvent.toHolidayItem(zone)` mengubah hasilnya jadi `HolidayItem`
  untuk adapter yang sama dengan hari besar.
- Navigasi:
  - `MainActivity.tvLabelCalendar` / `tvLabelDetailCalendar` →
    `openCalendarPage()` → `Intent` ke `CalendarActivity` **dengan** extra
    `LATITUDE`/`LONGITUDE` (double, dari `MainActivity.latitude/longitude`
    yang sudah terisi dari lokasi user).
  - `MainActivity.btnSeeAllHolidays` (di `setupHolidayPreview()`) →
    `openCalendarPage()` juga (sebelumnya `Intent` polos tanpa extra
    lat/lng; sudah diperbaiki bersamaan fitur Event Besar).
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
- `MainViewModel.fetchUpcomingEvents()` **memfilter** hanya hari
  yang `hijri.holidays` tidak kosong (bulan berjalan + bulan depan), digabung
  dengan event astronomi, ambil 3 teratas ke depan → jadi
  `HolidayItem` (dipakai `HolidayAdapter`, list card).
- `CalendarActivity.fetchYearlyHolidays()` melakukan filter yang sama
  (`hijri.holidays` tidak kosong) tapi memanggil `getIslamicHolidays()`
  berulang untuk semua bulan sisa tahun berjalan, tanpa batas jumlah item,
  plus fitur cari (`etSearch`) dan filter rentang tanggal
  (`btnStartDate`/`btnEndDate`).

### Event Besar: fenomena astronomi (`AstronomicalEventCalculator`)

Dua sumber data berbeda hidup berdampingan dalam satu list: hari besar
(Aladhan/Nager.Date, butuh internet) dan fenomena astronomi (hisab lokal
memakai Astronomy Engine `utils/Astronomy.kt`, **tanpa API**, jadi tetap
tampil offline — kalau Aladhan gagal, `CalendarActivity` & preview beranda
tidak lagi error total, event astronomi tetap muncul). Keduanya dibedakan
lewat `HolidayItem.jenis` (`EventJenis.HARI_BESAR` / `ASTRONOMI`, default
`HARI_BESAR` supaya semua pemanggil lama tetap valid) dan `HolidayItem.catatan`
(baris penjelasan tambahan, hanya diisi event astronomi; `tvNote` di
`item_holiday.xml`, disembunyikan kalau kosong). Keputusan desain (dari
pertanyaan terbuka di task Notion): **satu daftar dengan filter jenis**, bukan
dua halaman terpisah.

Cakupan `calculate()`:

| Kategori (`AstronomiKategori`) | Cara hitung | Bergantung lokasi? |
|---|---|---|
| `HARI_TANPA_BAYANGAN` | Tiap hari lokal disampel pada kulminasi Matahari (`searchHourAngle(Sun, hourAngle=0)`), lalu dicari hari dengan \|lintang − deklinasi Matahari\| minimum lokal ≤ 0,27° (jari-jari sudut Matahari) | **Ya** — hanya muncul di lintang tropis (dua kali setahun; Tokyo/Melbourne tidak dapat) |
| `EKUINOKS_SOLSTIS` | `seasons(year)` | Tidak |
| `PURNAMA` | Purnama (`searchMoonPhase(180°)`) terdekat ke ekuinoks September = Purnama Panen / Harvest Moon | Tidak |
| `OPOSISI_PLANET` | `searchRelativeLongitude(planet, 0°)` untuk Mars, Jupiter, Saturnus, Uranus, Neptunus | Tidak |
| `HUJAN_METEOR` | Tanggal Matahari mencapai λ☉ puncak (`searchSunLongitude`) untuk 12 hujan meteor utama; tabel λ☉ mengacu kalender IMO 2026 | Tidak |
| `GERHANA` | `lunarEclipsesAfter` + `globalSolarEclipsesAfter` (global; keterlihatan per lokasi tetap di menu Gerhana) | Tidak |

Catatan penting:

- Jam pada keterangan diformat di zona waktu perangkat. Singkatan zona
  Indonesia (`Asia/Jakarta`/`Pontianak` → WIB, `Makassar` → WITA, `Jayapura` →
  WIT) dipetakan manual karena emulator/perangkat tanpa data locale `id`
  mengembalikan "GMT+07:00". Zona lain jatuh ke nama bawaan sistem.
- Puncak hujan meteor adalah **perkiraan** dari λ☉ (waktu puncak nyata bisa
  meleset) — jam sengaja tidak ditampilkan untuk kategori ini.
- **Hujan Meteor Sextantid**: sumber tidak konsisten. Kalender IMO 2022 memberi
  puncak 27 September (λ☉ 184,3°), sedangkan kalender IMO 2026 (dan daftar
  Wikipedia) memberi 1 Oktober (λ☉ 188°). Kode memakai nilai IMO 2026 (188°);
  task Notion asli menyebut 27 September. Tandai "puncaknya tidak pasti" di
  keterangannya. Ganti `METEOR_SHOWERS` kalau kelak ada rujukan yang lebih
  pasti.
- **Okultasi Venus oleh Bulan sengaja belum digabung.** `OccultationCalculator`
  memindai konjungsi tiap 6 jam dengan paralaks topocentric — terlalu berat
  dijalankan tiap kali beranda dibuka, dan API-nya berhenti di 3 konjungsi
  pertama (termasuk yang bukan okultasi nyata), bukan per rentang tanggal.
  Fenomena itu tetap hanya ada di menu Okultasi.
- Kata "Purnama Panen" mengikuti istilah belahan bumi utara; secara astronomi
  cuma "purnama terdekat ke ekuinoks September".

### `CalendarActivity` = halaman list "lihat semua", bukan grid bulan penuh

`CalendarActivity` **tidak** merender grid kalender apa pun (tidak ada
`CalendarAdapter`/`GridLayoutManager` di file ini maupun di
`activity_calendar.xml`) — judulnya "Kalender & Hari Besar" tapi isinya murni
`RecyclerView` list (`HolidayAdapter`) berisi hari besar mendatang, dengan
search box + filter tanggal. Grid bulanan (visual kalender 7 kolom) **hanya**
ada di widget beranda `MainActivity` (`rvWeeklyCalendar` + `CalendarAdapter`),
tidak ada di `CalendarActivity`.

### Terjemahan nama hari besar Hijriah (`HijriHolidayTranslator`)

Nama hari besar dari `hijri.holidays` (Aladhan) berbahasa Inggris apa adanya,
termasuk banyak entri "Urs of Shaykh X" (haul wafat ulama tarekat) yang
variasi namanya sangat banyak. `utils/HijriHolidayTranslator.kt`
menerjemahkannya sebelum ditampilkan, dipanggil dari tiga tempat yang
sebelumnya masing-masing melakukan `joinToString(", ")` mentah:
`MainViewModel.fetchPrayerData()` (toast `holidayAlert`),
`MainViewModel.fetchUpcomingEvents()` (preview beranda), dan
`CalendarActivity.fetchYearlyHolidays()` (list lengkap). Dua lapis aturan:

- `exactTranslations`: map manual presisi untuk hari besar Islam umum (Idul
  Fitri, Idul Adha, Maulid Nabi, Isra Mi'raj, Tahun Baru Islam, Asyura, dll).
- `titleReplacements`: pattern replace umum ("Urs of" → "Haul", "Shaykh" →
  "Syaikh", "Mawlānā"/"Mawlana" → "Maulana") — nama diri ulama dibiarkan apa
  adanya karena daftarnya terlalu panjang untuk di-hardcode satu per satu.

Nama yang tidak cocok dengan aturan mana pun dikembalikan apa adanya (fallback
aman, tidak ada data yang hilang).

### Hari libur nasional Indonesia (Nager.Date, bukan lagi dead code)

`api/HolidayApiService.kt` sebelumnya berisi dead code (`HolidayApi`,
`HolidayRetrofitClient`, base URL `https://api-harilibur.vercel.app/`) yang
tidak dipakai di mana pun — API itu juga sudah tidak aktif (balas "Payment
required / DEPLOYMENT_DISABLED" saat dicek). Dead code itu sudah dihapus;
file ini sekarang hanya berisi `HolidayItem` (data class yang tetap dipakai
luas oleh `CalendarAdapter`, `HolidayAdapter`, `MainViewModel`,
`CalendarActivity`).

Sebagai gantinya, `api/NationalHolidayApiService.kt` (interface
`NationalHolidayApi`, object `NationalHolidayRetrofitClient`, base URL
`https://date.nager.at/`) dipakai di `CalendarActivity.fetchYearlyHolidays()`
untuk menambah hari libur nasional non-Islam (Tahun Baru Masehi, Wafat Isa
Almasih, Paskah, Hari Buruh, Kenaikan Isa Almasih, Hari Lahir Pancasila, HUT
RI, Natal) ke `allHolidays`, digabung dengan hari besar Islam dari Aladhan
sebelum di-sort. `localName` dari Nager.Date sudah dalam Bahasa Indonesia,
jadi tidak perlu translasi tambahan. Cakupannya terbatas ke hari libur
tetap/global saja — **tidak** termasuk cuti bersama maupun hari libur daerah,
dan hari besar Islam (Idul Fitri, dll) tetap dari Aladhan seperti biasa
(tidak dobel karena Nager.Date tidak menyertakannya). Fetch ini dibungkus
`try/catch` terpisah — kalau gagal (API down dsb.), hari besar Islam tetap
tampil normal. Panggilan ini **hanya** ada di `CalendarActivity`, belum di
preview beranda (`MainViewModel.fetchUpcomingEvents()`) maupun
`checkNationalHoliday()` (lihat known issue terkait di bawah, belum berubah).

File yang terlibat:

| File | Peran |
|---|---|
| `MainActivity.kt` | Host widget kalender bulanan (`rvWeeklyCalendar`) & preview hari besar (`rvHolidayPreview`) di beranda; trigger `initCalendar`/`fetchUpcomingEvents` saat lokasi didapat; navigasi ke `CalendarActivity` |
| `viewmodel/MainViewModel.kt` | State beranda: `calendarData`/`hijriTitle`/`monthYearTitle` (grid), `holidayPreview` (list 3 item), `holidayAlert` (toast hari ini); logic `fetchMonthlyCalendar()` & `fetchUpcomingEvents()`; menerjemahkan nama hari besar lewat `HijriHolidayTranslator` |
| `repo/PrayerRepository.kt` (`getIslamicHolidays`) | Jembatan tunggal ke Aladhan `api.getCalendar(lat, lng, method, month, year, methodSettings)` — dipakai oleh grid kalender, preview hari besar, dan `CalendarActivity` |
| `adapter/CalendarAdapter.kt` | Adapter grid 7 kolom (`DayUIModel`: tanggal Masehi, tanggal Hijriah, flag hari ini/hari besar/slot kosong) — dipakai di `MainActivity` saja |
| `adapter/HolidayAdapter.kt` | Adapter list card hari besar (`HolidayItem`: tanggal, tanggal Hijriah, keterangan) — dipakai di `MainActivity` (preview) dan `CalendarActivity` (list lengkap) |
| `ui/calendar/CalendarActivity.kt` | Halaman "lihat semua hari besar": ambil data bulan berjalan s.d. Desember dari Aladhan + hari libur nasional dari Nager.Date, search + filter rentang tanggal, terjemahkan nama lewat `HijriHolidayTranslator` |
| `api/HolidayApiService.kt` | Berisi `HolidayItem` (data class, dipakai luas). Dead code `HolidayApi`/`HolidayRetrofitClient` sudah dihapus |
| `api/NationalHolidayApiService.kt` | `NationalHolidayApi`/`NationalHolidayRetrofitClient` ke Nager.Date (`date.nager.at`) untuk hari libur nasional Indonesia |
| `utils/HijriHolidayTranslator.kt` | Terjemahan nama hari besar Hijriah Inggris → Indonesia (exact map + pattern replace) |
| `utils/AstronomicalEventCalculator.kt` | Hisab lokal fenomena astronomi (Hari Tanpa Bayangan, ekuinoks/solstis, Purnama Panen, oposisi planet, hujan meteor, gerhana) + ekstensi `toHolidayItem()` |
| `model/EventBesarModels.kt` | `EventJenis`, `AstronomiKategori`, `AstronomicalEvent` |
| `app/src/test/.../utils/AstronomicalEventCalculatorTest.kt` | Golden test terhadap data riset Notion (Sept–Okt 2026) + invariant |
| `res/layout/activity_calendar.xml` | Layout `CalendarActivity`: header, search box, date range filter, `RecyclerView` list — tanpa grid |

Alur data (grid kalender beranda): `MainActivity.fetchDataByCoordinate()` →
`viewModel.initCalendar(lat, lng)` → `fetchMonthlyCalendar()` →
`PrayerRepository.getIslamicHolidays()` → Aladhan `getCalendar` → diproses
jadi `List<DayUIModel>` (isi 1 bulan penuh + slot kosong padding awal
minggu) → LiveData `calendarData` → `MainActivity` bind ke `CalendarAdapter`
lewat `GridLayoutManager(7)`. Navigasi bulan (`changeMonth`) mengulang alur
yang sama dengan `currentCalendar` yang sudah digeser.

Alur data (preview event besar beranda): `fetchDataByCoordinate()` →
`viewModel.fetchUpcomingEvents(lat, lng)` → panggil
`getIslamicHolidays()` untuk bulan berjalan dan bulan depan (masing-masing
`try/catch` sendiri) → filter hari yang punya `hijri.holidays`; lalu
`AstronomicalEventCalculator.calculate()` untuk 60 hari ke depan → gabung,
filter tanggal >= hari ini, urutkan, `take(3)` →
`List<HolidayItem>` → LiveData `holidayPreview` → `MainActivity` bind ke
`HolidayAdapter` di `rvHolidayPreview`. `Resource.Error` hanya kalau
gabungannya kosong.

Alur data (halaman lihat semua): `CalendarActivity.onCreate()` → baca extra
`LATITUDE`/`LONGITUDE` dari `Intent` (default Jakarta kalau tidak ada) →
`fetchYearlyHolidays()` → loop `getIslamicHolidays()` per bulan (bulan
berjalan s.d. Desember tahun berjalan) → gabung semua hasil, filter tanggal
>= hari ini, urutkan → `originalList` → `applyFilter()` → `HolidayAdapter` di
`rvHolidays` (event astronomi ditambahkan ke `allHolidays` sebelum langkah
ini, dari hari ini s.d. 1 Januari tahun depan).
Search (`etSearch`) dan filter tanggal (`btnStartDate`/`btnEndDate`) bekerja
di atas `originalList` yang sudah ada di memori (`applyFilter()`), tidak
memanggil API lagi.

## 5. Dependencies & tech stack khusus

Tidak ada tambahan khusus di luar stack umum app — Retrofit + Gson (Aladhan
API, sama dengan fitur Waktu Sholat), `RecyclerView`/`GridLayoutManager` dari
AndroidX, dan `DatePickerDialog` bawaan Android untuk filter rentang tanggal
di `CalendarActivity`.

## 6. Testing

Bagian **event astronomi** punya test otomatis: `AstronomicalEventCalculatorTest`
(16 test, golden/reference tanpa mock — lihat `docs/strategi-unit-test.md`
Jalur 1). Contoh yang dicek: Hari Tanpa Bayangan Pontianak 23 Sep 2026,
Jakarta ±9 Okt, Surabaya ±12 Okt; ekuinoks 23 Sep; Purnama Panen & oposisi
Neptunus 26 Sep; tidak ada Hari Tanpa Bayangan di Tokyo/Melbourne; deteksi di
tepi jendela; label zona WIB/WITA. Bagian hari besar Islam/nasional (Aladhan,
Nager.Date, `HijriHolidayTranslator`) dan UI **belum** punya test otomatis.
Verifikasi manual:

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
7. Tap chip "Astronomi" / "Hari Besar" / "Semua" → list hanya berisi jenis
   yang dipilih (kartu astronomi punya ikon kilau + baris catatan jam
   puncak). Buka dengan extra lokasi lain (mis. `adb shell am start -n
   site.elahady.alkaukaba/.ui.calendar.CalendarActivity --ed LATITUDE -0.0267
   --ed LONGITUDE 109.3425` untuk Pontianak) → tanggal Hari Tanpa Bayangan
   berubah sesuai lintang.

## 7. Known issues & TODOs

- [x] ~~**Extra `LATITUDE`/`LONGITUDE` hilang di jalur `btnSeeAllHolidays`.**~~
      Sudah diperbaiki: listener-nya sekarang memakai `openCalendarPage()`
      seperti dua jalur lain. Penting sejak ada Hari Tanpa Bayangan, karena
      tanpa extra itu `CalendarActivity` jatuh ke default Jakarta.
- [x] ~~**`api/HolidayApiService.kt` berisi dead code.**~~ Sudah diganti:
      dead code `HolidayApi`/`HolidayRetrofitClient` (API mati
      `api-harilibur.vercel.app`) dihapus, diganti
      `api/NationalHolidayApiService.kt` ke Nager.Date (`date.nager.at`,
      gratis tanpa key) — dipakai di `CalendarActivity.fetchYearlyHolidays()`
      untuk hari libur nasional non-Islam. **Belum** dipakai di
      `checkNationalHoliday()` (lihat poin di bawah) maupun preview beranda.
- [ ] **`checkNationalHoliday()` cuma hardcode 2 hari libur nasional**
      (Kemerdekaan RI, Tahun Baru Masehi) sebagai fallback toast hari besar
      kalau hari ini bukan hari besar Hijriah. Tidak mencakup hari libur
      nasional lain (Natal, Waisak, dll) — sekarang `CalendarActivity` sudah
      punya sumber datanya (`NationalHolidayRetrofitClient`, lihat di atas),
      tinggal disambungkan ke sini juga kalau mau toast harian konsisten
      dengan list lengkap.
- [ ] **Cakupan Nager.Date terbatas ke hari libur tetap/global** — dicek
      manual untuk 2025 & 2026, cuma 8 entri (Tahun Baru Masehi, Wafat Isa
      Almasih, Paskah, Hari Buruh, Kenaikan Isa Almasih, Hari Lahir Pancasila,
      HUT RI, Natal). **Tidak** termasuk Nyepi, Waisak, Imlek, cuti bersama,
      maupun hari libur daerah/adat. Kalau butuh cakupan resmi penuh (SKB 3
      Menteri), perlu sumber data lain.
- [ ] **`CalendarActivity` hanya mengambil hari besar s.d. Desember tahun
      berjalan** (`fetchYearlyHolidays()`, loop `currentMonth..12`). Kalau
      dibuka di bulan Desember dan sudah tidak ada hari besar tersisa tahun
      itu, list akan kosong (toast "Tidak ada data") — tidak otomatis lanjut
      ke tahun berikutnya.
- [x] ~~Preview hari besar hanya mengambil bulan berjalan.~~ Sekarang bulan
      berjalan + bulan depan (`fetchUpcomingEvents()`), digabung event
      astronomi 60 hari. Sisa keterbatasan: hari besar Islam >1 bulan ke depan
      tidak masuk preview (mereka tetap ada di halaman "lihat semua").
- [ ] Belum ada test otomatis untuk sisi hari besar Islam/nasional dan UI
      (lihat bagian Testing di atas; event astronomi sudah ter-test).
- [ ] **Okultasi Venus belum masuk daftar Event Besar** — lihat catatan di
      section 4 (butuh varian `OccultationCalculator` yang per-rentang tanggal
      dan lebih ringan).
- [ ] **Nilai puncak Sextantid perlu dikonfirmasi** ke sumber IMO terbaru
      (27 Sep λ☉ 184,3° vs 1 Okt λ☉ 188°) — lihat section 4.
- [ ] **Event astronomi ikut berhenti di akhir tahun berjalan**, sama seperti
      hari besar — kalau known issue Desember di atas diperbaiki, ubah juga
      `startOfNextYear` di `CalendarActivity.fetchYearlyHolidays()`.
- [ ] Belum ada notifikasi/toast untuk event astronomi hari ini (toast
      `holidayAlert` masih khusus hari besar Islam/nasional).
- [ ] Zona waktu jam pada event memakai zona perangkat, bukan zona lokasi
      override (mis. pilih lokasi di zona lain lewat Konfigurasi).
