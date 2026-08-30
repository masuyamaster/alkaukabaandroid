# Roadmap Clean Code

Catatan kelanjutan dari diskusi soal "saya ingin project ini clean code, mulai
dari mana?". Dokumen ini bukan doc per-fitur (lihat `features/` untuk itu),
tapi daftar keputusan/rencana lintas-fitur soal kualitas kode, supaya diskusi
ini ada follow-up-nya di sesi berikutnya.

Prinsip yang disepakati: **jangan mulai dari restrukturisasi besar** (Clean
Architecture penuh dengan layer `data/domain/presentation` + DI framework).
Untuk project solo seperti ini itu overkill. Mulai dari hygiene fixes yang
sudah kelihatan nyata di kode, baru masuk ke lapisan domain/DI kalau memang
sudah terasa perlu.

## Selesai

- [x] **Package declaration `PrayerRepository.kt`.** File ini tadinya tidak
  punya `package` sama sekali (default package), bikin kapt warning
  ("Can't reference type 'PrayerRepository' from default package") dan semua
  pemanggil harus `import PrayerRepository` tanpa prefix. Sudah dibetulkan
  jadi `package site.elahady.alkaukaba.repo`, 7 file pemanggil diupdate.
- [x] **Pisahkan business logic dari `WaktuSholatActivity`.** Dulu
  `updateNextPrayerUI()` mencampur perhitungan "sholat mana yang aktif
  sekarang" (business logic) dengan manipulasi View. Sekarang perhitungan itu
  pindah ke `PrayerTimesViewModel.buildSchedule()`, menghasilkan
  `PrayerScheduleUiState` siap-render; Activity cuma bind ke View. Detail di
  [features/waktu-sholat.md](features/waktu-sholat.md#3-titik-masuk-logika--navigasi).
- [x] **Breakdown kalkulasi teknis, khusus untuk Ephemeris (tapi extensible).**
  User kasih contoh nyata cara hitung manual (kertas "Hisab Waktu Sholat
  Ephimeris" - Kulminasi, koreksi Equation of Time, Kwd, Ikhtiyat per waktu
  sholat) dan menegaskan: breakdown ini **hanya untuk Ephemeris sekarang**,
  tapi arsitekturnya **tidak boleh hardcode ke satu metode** - harus gampang
  ditambah ke metode lain nanti kalau dibutuhkan.

  Yang dibangun (bukan sekadar rancangan lagi):
  - `utils/prayerbreakdown/PrayerCalculationBreakdownProvider` - interface
    titik-ekstensi (`fun breakdown(lat, lng, timeZoneHour): List<PrayerBreakdownSection>`).
  - `utils/prayerbreakdown/PrayerCalculationBreakdownRegistry` - `Map<methodId, Provider>`.
    Ini titik ekstensinya: nambah breakdown ke metode lain = tambah satu baris
    di map ini, tidak sentuh ViewModel/Activity sama sekali.
  - `utils/prayerbreakdown/EphemerisPrayerCalculator` - satu-satunya provider
    hari ini, didaftarkan untuk `PrayerCalculationMethods.EPHEMERIS_ID`.
    Formulanya diadaptasi dari `PrayerTextCalculator.kt` lama (sudah dihapus,
    tergantikan sepenuhnya oleh ini) - deklinasi & Equation of Time pakai
    pendekatan sinusoidal sederhana ("Ephemeris Approximation", didisclose
    apa adanya di UI), BUKAN tabel ephemeris presisi tinggi seperti contoh
    kertas dari user (yang pakai data Buku Ephemeris + algoritma Jean Meeus).
    Kalau nanti ada sumber data matahari yang lebih presisi, ganti isi kelas
    ini saja - struktur breakdown/registry tidak perlu berubah.
  - `WaktuSholatActivity` tab "Detail Perhitungan" (sekarang betulan bisa
    diakses - `tabContainer` & `btnTabDetail` yang tadinya `visibility="gone"`
    sudah dibuka) merender accordion (`item_prayer_breakdown.xml`, expand per
    waktu sholat) kalau `PrayerCalculationBreakdownRegistry.providerFor(methodId)`
    mengembalikan provider, atau pesan fallback ("metode ini tidak punya
    breakdown detail") kalau `null` - **bukan** `if (method == EPHEMERIS_ID)`
    yang di-hardcode di Activity.

  **Sudah diverifikasi di emulator kedua arahnya**: pilih Ephemeris →
  accordion muncul dengan angka asli terhitung; pilih Muslim World League →
  section otomatis ganti jadi pesan fallback, tanpa ubah kode apa pun.

  **Catatan penting yang perlu disadari user**: karena breakdown Ephemeris ini
  pakai algoritma sendiri (sinusoidal approximation) sedangkan jadwal di tab
  "Waktu Aktual" untuk Ephemeris masih bersumber dari Aladhan API method
  Kemenag RI (lihat bagian fallback Ephemeris di
  [features/waktu-sholat.md](features/waktu-sholat.md)), **kedua angka bisa
  beda beberapa menit** untuk waktu sholat yang sama (mis. Dzuhur 11:31 dari
  Aladhan vs breakdown lokal juga kebetulan 11:31, tapi Ashar 14:49 dari
  Aladhan vs 14:50 dari breakdown lokal). Ini bukan bug, tapi konsekuensi dari
  dua sumber angka berbeda yang belum disatukan - baru akan konsisten kalau
  Ephemeris beneran menjadi satu-satunya sumber (lihat item TODO di
  `features/waktu-sholat.md`).

- [x] **Top bar & window insets konsisten di semua layar (fix overlap nav bar,
  shadow bottom sheet jelek, top bar tidak seragam).** Dipicu laporan user
  lewat 3 screenshot: kartu Kalender & tombol "Hitung Ulang" ketutupan gesture
  nav bar, shadow di bottom sheet "Detail Perhitungan Arah Kiblat" nongol
  kotak di balik kartu bulat, dan tiap activity punya gaya top bar/back button
  sendiri-sendiri.

  **Root cause overlap nav bar**: semua activity manggil
  `WindowCompat.setDecorFitsSystemWindows(window, false)` (edge-to-edge) tapi
  **tidak ada satupun** yang consume `WindowInsetsCompat` - padding
  bawah/atas yang ada selama ini cuma angka tebakan manual (`paddingTop=20dp`,
  `marginTop=48dp`, dst), tidak dinamis mengikuti tinggi bar sungguhan di
  device. `AwalBulanActivity` malah tidak manggil `setDecorFitsSystemWindows`
  sama sekali, tapi tetap kena edge-to-edge karena `targetSdk 36` (Android 15+
  mulai mem-force edge-to-edge terlepas dari pemanggilan itu).

  **Root cause shadow bottom sheet**: `BottomSheetDialog` bawaan Material
  Components punya container (`design_bottom_sheet`) bersudut persegi; kode
  lama nutup warnanya jadi transparan (`setBackgroundColor(TRANSPARENT)`) dan
  gambar sudut membulat sendiri di konten satu level di dalamnya
  (`bg_card_up_rounded`). Elevation shadow tetap mengikuti outline persegi si
  container, jadi nongol di balik kartu yang membulat.

  Yang dibangun:
  - `utils/InsetsUtils.kt` - dua extension function (`applySystemBarInsetsPadding`,
    `applyTopSystemBarInsetAsMargin`) yang menambah inset system bar **di atas**
    padding/margin dasar yang sudah ada di XML, dipasang di titik
    scroll/tombol/container paling bawah (dan atas untuk toolbar) tiap activity.
  - `res/layout/view_toolbar_default.xml` - komponen top bar bersama (tombol
    back bulat + judul + slot ikon aksi opsional), dipasang via `<include>` di
    `KonfigurasiActivity`, `WaktuSholatActivity`, `AwalBulanActivity`, dan
    `KiblatActivity` (yang terakhir tadinya pakai `Toolbar` asli dengan
    back-press ganda - `OnBackPressedDispatcher` **dan** override
    `onBackPressed()` deprecated sekaligus - sekarang disatukan jadi satu
    jalur). **Keputusan user**: halaman `CalendarActivity` sengaja **tidak**
    ikut dikonversi ke komponen ini - tetap pakai header gradient teal seperti
    dashboard, cuma margin/padding-nya dibikin dinamis lewat `InsetsUtils`.
    `MainActivity` (dashboard) juga tidak disentuh top bar-nya (branded, tanpa
    back button), cuma overlap bawahnya yang difix.
  - `themes.xml` - `bottomSheetDialogTheme` baru
    (`ThemeOverlay.AlKaukaba.BottomSheetDialog` → `Widget.AlKaukaba.BottomSheet.Modal`
    → `ShapeAppearance.AlKaukaba.BottomSheet`, sudut atas 16dp) supaya
    `design_bottom_sheet` sendiri yang membulat dan shadow ikut bentuknya.
    Efeknya app-wide - semua `BottomSheetDialog` (termasuk 3 dialog di
    `KonfigurasiActivity`) kebagian fix ini, bukan cuma dialog Kiblat. Hack
    `setBackgroundColor(TRANSPARENT)` yang lama dihapus di 4 titik karena
    sekarang jadi kontraproduktif (menutup shadow yang sudah dibetulkan).

  **Diverifikasi**: build `assembleDebug` + install ke HP fisik (bukan cuma
  emulator) via `adb install -r`, bukan sekadar compile check.

  **Known gap**: `LoginActivity` dan `Splashscreen` belum disentuh (tidak ada
  laporan masalah di situ, dan `LoginActivity` memang tidak punya back
  button). Kalau nanti ada activity baru yang butuh top bar dengan back
  button, pakai `view_toolbar_default.xml` + `InsetsUtils` supaya konsisten,
  jangan hand-roll header baru lagi.

## Belum waktunya, jadi konsesi untuk nanti

- [ ] **Domain layer tipis untuk logika yang punya business rule nyata.**
  Bukan untuk semua fitur — cuma bagian yang benar-benar ada percabangan
  logika yang layak dites (mis. perhitungan waktu sholat, fallback Ephemeris,
  nanti breakdown Ephemeris di atas). Baru dikerjakan kalau sudah mulai terasa
  butuh unit test yang mudah, bukan sekadar ikut pola "seharusnya begitu".
- [ ] **DI framework (Hilt/Koin).** Baru relevan kalau jumlah Factory manual
  (`PrayerViewModelFactory`, `MainViewModelFactory`, dst) mulai berat dirawat.
  Belum darurat sekarang — masih cuma segelintir Factory, semuanya pola yang
  sama persis.

Kalau nanti mau lanjut ke salah satu poin di atas, buka lagi dokumen ini dulu
supaya konteks kenapa ditunda tidak hilang.
