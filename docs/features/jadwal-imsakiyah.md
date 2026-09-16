# Jadwal Imsakiyah

## 1. Ringkasan

**Fitur**: Jadwal Imsakiyah — tabel semua waktu sholat (Sepertiga Malam
Akhir, Imsak, Subuh, Dhuha, Dzuhur, Ashar, Maghrib, Isya) untuk 1 bulan
Hijriyah penuh, satu baris per hari. Menjawab kebutuhan umum "jadwal
imsakiyah Ramadhan", tapi tidak dikunci ke bulan Ramadhan saja — user bisa
geser ke bulan Hijriyah manapun lewat navigasi ◀/▶.

Kolom tanggal (kolom pertama) **freeze** — tetap diam di kiri layar saat
kolom-kolom waktu sholat digeser horizontal (lihat section 4).

## 2. Entry point & prasyarat

- Layar: `JadwalImsakiyahActivity` (layout `activity_jadwal_imsakiyah.xml`).
- Dibuka dari tombol "Jadwal Imsakiyah" di home (`bt_jadwal_imsakiyah`,
  posisi setelah "Hisab Nasional") dan dari "Semua Menu" (posisi sama,
  setelah "Hisab Awal Bulan Nasional").
- Per 2026-09-16: menu "Doa & Dzikir" dihapus dari grid home (2x4, sudah
  penuh — lihat `docs/features/hisab-nasional.md` §2/§7) untuk kasih tempat
  menu ini; "Doa & Dzikir" **masih ada** di "Semua Menu", tidak dihapus dari
  aplikasi.
- Prasyarat: sama seperti Waktu Sholat — lokasi (GPS + permission, atau
  lokasi manual dari Konfigurasi, fallback Jakarta) dan koneksi network
  (data diambil dari Aladhan API).

## 3. Titik masuk logika & navigasi

- `HijriCalendarEngine.monthRangeForOffset(observer, referenceDate,
  monthOffset): HijriMonthRange` (baru, `utils/HijriCalendarEngine.kt`) —
  titik masuk untuk "kasih tanggal 1 Masehi + jumlah hari 1 bulan Hijriyah
  yang digeser N bulan dari bulan yang memuat `referenceDate`". Reuse mesin
  hisab yang sama dengan kalender home/Awal Bulan (ijtima' + ghurub +
  kriteria Neo-MABIMS + istikmal), bukan `HijriDateUtil` tabular.
- `JadwalImsakiyahViewModel.loadMonth(lat, lng, offset)` — entry point utama
  fitur, dipanggil ulang oleh `nextMonth()`/`prevMonth()` (state
  `monthOffset`/`lat`/`lng` disimpan di ViewModel).
- Navigasi ke layar ini: `Intent(context, JadwalImsakiyahActivity::class.java)`
  dari `MainActivity`/`SemuaMenuActivity`. Tidak ada navigasi keluar dari
  layar ini selain tombol back toolbar.

## 4. Struktur & alur data

| File | Peran |
|---|---|
| `ui/jadwalimsakiyah/JadwalImsakiyahActivity.kt` + `activity_jadwal_imsakiyah.xml` | UI: toolbar, baris navigasi bulan (◀ label ▶), tabel (di-build programatic ke `layoutImsakiyahTable`). Resolusi lokasi (GPS/manual/fallback) dicopy dari `WaktuSholatActivity`. |
| `viewmodel/jadwalimsakiyah/JadwalImsakiyahViewModel.kt` + `JadwalImsakiyahViewModelFactory.kt` | `LiveData<ImsakiyahUiState>` (monthLabel, columnLabels, rows), `isLoading`, `errorMessage`. `ImsakiyahRow(hijriDay, gregorianLabel, times)`. |
| `res/layout/item_imsakiyah_day_cell.xml` | 1 sel kolom tanggal **freeze** (`TextView` tunggal, 56dp x 52dp), diinflate berulang ke `layoutImsakiyahDayColumn` — di luar `HorizontalScrollView` supaya tidak ikut geser. |
| `res/layout/item_imsakiyah_row.xml` | 1 baris 8 kolom waktu (tanpa kolom tanggal lagi sejak freeze-column), diinflate berulang ke `layoutImsakiyahTable` di dalam `HorizontalScrollView`, untuk header (bold + tint amber) dan tiap hari (background selang-seling). Row height di-fixed 52dp di kedua layout (`item_imsakiyah_day_cell.xml` & `item_imsakiyah_row.xml`) supaya baris tanggal & baris waktu tetap sejajar saat scroll vertikal. |
| `utils/HijriCalendarEngine.kt` | + `HijriMonthRange` + `monthRangeForOffset()` (public, baru) + `previousSegment()` (private, baru) — logic inti tetap yang lama (`findSegmentContaining`/`nextSegment`/dst), tidak diubah. |
| `api/PrayersApiService.kt` | `Timings` (dipakai response `/v1/calendar`) ditambah field opsional `imsak`/`sunrise`/`lastThird` (additive, field lama tidak berubah) — sebelumnya cuma `Fajr/Dhuhr/Asr/Maghrib/Isha`. |
| `repo/PrayerRepository.kt` | Tidak berubah — reuse `getIslamicHolidays(lat, lng, month, year)` yang sudah ada (dipanggil 1-2x, dedup per bulan Masehi yang dilewati rentang bulan Hijriyah). |
| `viewmodel/waktusholat/PrayerTimesViewModel.kt` | Reuse `PrayerKind` enum (urutan kolom) — tidak diubah. |

Alur data: `JadwalImsakiyahActivity` resolve lokasi -> `viewModel.loadMonth(lat,
lng, 0)` -> `HijriCalendarEngine.monthRangeForOffset()` (dapat tanggal 1 +
jumlah hari) -> kumpulkan pasangan (bulan, tahun) Masehi unik yang dilewati
rentang itu -> `PrayerRepository.getIslamicHolidays()` per pasangan (1-2
panggilan network, BUKAN loop per-hari 29/30x) -> gabung hasil, cocokkan per
tanggal (pola sama `MainViewModel.fetchMonthlyCalendar()`: parse
`PrayerData.date.readable` format `dd MMM yyyy` EN, banding string
`yyyy-MM-dd`) -> `ImsakiyahUiState` -> Activity flatten ke dua container
terpisah (`renderTable()`), bukan satu (lihat revisi "kolom tanggal freeze"
di bawah).

**Struktur layout tabel (`activity_jadwal_imsakiyah.xml`)** — kolom tanggal
freeze, sisanya scroll:

```
ScrollView (vertikal, weight=1)
  LinearLayout horizontal, background=bg_card_rounded, clipToOutline=true  <- "kartu"
    LinearLayout id=layoutImsakiyahDayColumn (vertikal)   <- FREEZE, di luar HorizontalScrollView
    View (divider 1dp)
    HorizontalScrollView
      LinearLayout id=layoutImsakiyahTable (vertikal)     <- 8 kolom waktu, ikut geser
```

Karena kolom tanggal & tabel waktu adalah dua `LinearLayout` vertikal
terpisah yang jadi children horizontal dari kartu yang sama, keduanya tetap
scroll vertikal bersamaan (satu `ScrollView` membungkus semuanya) — tapi
cuma `layoutImsakiyahTable` yang dibungkus `HorizontalScrollView`, jadi
geser horizontal hanya menggerakkan kolom 2 dst. `JadwalImsakiyahActivity.
buildDayCell()`/`buildHeaderRow()`/`buildDataRow()` mengisi kedua container
ini secara paralel per baris (index yang sama -> warna selang-seling yang
sama), bukan lagi satu `buildHeaderRow()`/`buildDataRow()` yang mengisi
kolom tanggal+waktu sekaligus seperti versi awal.

Dhuha dihitung dari `Sunrise + 15 menit` (`DHUHA_OFFSET_MINUTES`), sama
persis seperti `PrayerTimesViewModel`. Kalau hari tertentu tidak ada data
match (network gagal sebagian/field null), sel ditampilkan "-", bukan crash.

## 5. Dependencies & tech stack khusus

Tidak ada tambahan khusus di luar stack umum app (Retrofit/Gson untuk
Aladhan API, `io.github.cosinekitty.astronomy` untuk `HijriCalendarEngine`
— keduanya sudah dipakai fitur lain).

## 6. Testing

Belum ada test otomatis (`HijriCalendarEngine.monthRangeForOffset()` maupun
`JadwalImsakiyahViewModel` belum ada unit test — konsisten dengan
`HijriCalendarEngine`/`EphemerisCalculator` lain yang juga belum ada test
JVM, lihat `docs/features/bulan-hijriyah.md` §7).

**Verifikasi manual (emulator Pixel_4_XL_API_36, 2026-09-16)**: install APK
debug, home -> tombol "Jadwal Imsakiyah" (setelah "Hisab Nasional", slot
"Doa & Dzikir" sudah tidak ada) -> tabel tampil (header "Tgl" + 8 kolom
waktu, tint amber), scroll vertical (30 baris) & horizontal (kolom) jalan
lancar tanpa lag. Tombol ▶ ganti bulan dari "Rabiul Akhir 1448 H" ke
"Jumadil Awal 1448 H", data reload benar (nilai waktu turun konsisten
hari-ke-hari, wajar secara musim). "Semua Menu" dicek juga: "Jadwal
Imsakiyah" muncul tepat setelah "Hisab Awal Bulan Nasional", "Doa & Dzikir"
masih ada & masih bisa dibuka.

**Revisi setelah verifikasi visual pertama** (2026-09-16, sama hari):
1. User minta kolom tanggal di-freeze (awalnya seluruh tabel termasuk
   kolom tanggal ikut geser horizontal jadi satu blok) — direstrukturisasi
   jadi 2 container terpisah (lihat section 4). Diverifikasi ulang: kolom
   "Tgl" diam saat swipe horizontal, baris tetap sejajar kiri-kanan saat
   swipe vertikal.
2. User laporkan "background tanggalnya overlapping layout" — kolom
   tanggal freeze (background flat per-sel, tanpa rounded corner) menonjol
   melewati sudut rounded kartu (`bg_card_rounded`, radius 24dp) di
   pojok kiri-atas, kelihatan seperti notch putih memotong header amber.
   Fix: `android:clipToOutline="true"` di kartu wrapper supaya semua
   children (termasuk kolom freeze) ke-clip ke outline rounded-nya. Sudah
   diverifikasi ulang via screenshot — sudut bersih, tidak ada notch.

## 7. Known issues & TODOs

- [ ] Tabel 9 kolom (Tgl + 8 waktu) cukup lebar — di layar sempit user harus
  scroll horizontal untuk lihat kolom Dzuhur-Isya, tidak ada indikator visual
  "geser ke kanan" selain scrollbar tipis bawaan `HorizontalScrollView`.
- [ ] Field `Imsak`/`Sunrise`/`Lastthird` di `Timings` baru pertama kali
  dipakai lewat endpoint `/v1/calendar` di fitur ini (sebelumnya endpoint itu
  cuma dipakai untuk deteksi hari libur di kalender home, jadi field-field
  itu tidak pernah diparse). Sudah diverifikasi manual di emulator hasilnya
  masuk akal (lihat section 6), tapi belum ada assertion otomatis kalau
  Aladhan suatu saat ubah shape response endpoint ini.
- [ ] Navigasi bulan cuma prev/next 1 langkah (bukan spinner ala
  `AwalBulanActivity`) — cukup untuk kasus pakai "cek beberapa bulan
  berdekatan", tapi lompat jauh (mis. dari bulan sekarang ke Ramadhan tahun
  depan) perlu tap berulang.
- [ ] State `monthOffset`/tabel cuma di memori ViewModel, hilang kalau
  Activity di-recreate/rotate — sama seperti `AwalBulanActivity`, dianggap
  cukup untuk kasus pakai utamanya.
