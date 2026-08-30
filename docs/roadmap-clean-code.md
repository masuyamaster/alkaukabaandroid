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

## Didesain, menunggu bahan dari Ephemeris

- [ ] **Breakdown kalkulasi teknis, khusus untuk Ephemeris.** Kebutuhan: kalau
  user pilih metode Ephemeris, ada tampilan detail "angka ini didapat dari
  mana" (breakdown rumus/langkah hitung). Metode lain (semua yang
  "(Aladhan API)") **tidak perlu** breakdown ini, karena angkanya memang
  murni dari response API, bukan hasil hitung lokal.

  Rancangan yang disarankan (belum diimplementasikan — menunggu rumus
  Ephemeris beneran dari Al Hasib/tim Alkaukaba):

  1. Buat kelas baru khusus, mis. `utils/ephemeris/EphemerisPrayerCalculator.kt`,
     tanggung jawab tunggal: hitung breakdown langkah demi langkah untuk
     Ephemeris. Jangan campur dengan `PrayerTextCalculator.kt` yang sudah ada
     sekarang (itu formula sudut matahari generik ala Kemenag, bukan
     Ephemeris/posisi bulan — kalau Ephemeris sudah nyata, evaluasi apakah
     `PrayerTextCalculator` masih perlu dipertahankan atau digantikan).
  2. Representasikan breakdown sebagai data terstruktur, bukan `String`
     mentah — mis. `data class EphemerisCalculationStep(val label: String, val formula: String, val result: String)`
     — supaya UI bisa render rapi per baris, bukan blob teks.
  3. Di `PrayerTimesViewModel`, expose `LiveData<List<EphemerisCalculationStep>?>`
     yang **null kalau method aktif bukan Ephemeris**, isi kalau Ephemeris.
  4. Di `WaktuSholatActivity`, tab/section "Detail Perhitungan"
     (`btnTabDetail`, saat ini malah disembunyikan total —
     `binding.btnTabDetail.visibility = View.GONE`) baru ditampilkan kalau
     LiveData di atas tidak null. Untuk method lain, tab ini tidak pernah
     muncul.

  Efeknya: begitu rumus Ephemeris siap, cuma perlu isi
  `EphemerisPrayerCalculator`, tidak perlu bongkar struktur UI lagi.

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
