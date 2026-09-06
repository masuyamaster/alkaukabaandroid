# Strategi Unit Test

Bukan per-fitur — ini catatan metodologi lintas-fitur: kapan pakai mock, kapan
tidak, alat apa untuk kasus apa, dan alur kerja menulis satu unit test di repo
ini. Ditulis setelah screening awal (2026-09-06) menemukan cuma
`utils/addurrulaniq/` yang punya test JVM nyata; area lain (ViewModel, Repo,
`EphemerisCalculator`, dll.) belum ada test otomatis sama sekali — lihat
backlog checklist "Tulis unit test bertahap" di Notion Pengembangan Al-Kaukaba
untuk status cakupan terkini (sengaja tidak diduplikasi di sini karena
berubah terus, gampang basi).

## Dua jalur, bukan satu resep

Kesalahan paling umum: menganggap "unit test" itu satu pendekatan yang sama
untuk semua kode. Di repo ini ada dua jalur berbeda, ditentukan dari satu
pertanyaan: **apakah kode yang ditest murni logic/kalkulasi, atau menyentuh
dependency eksternal (API, Context Android, sensor, singleton lain)?**

### Jalur 1 — Golden / Reference Test (tanpa mock)

Untuk kode murni kalkulasi (`utils/EphemerisCalculator`,
`utils/addurrulaniq/*`, `utils/prayerbreakdown`): **tidak pakai mock sama
sekali**. Input asli, hasil dibandingkan ke:
- Rentang fisis yang masuk akal (mis. `tinggiHilal in -90.0..90.0`), atau
- Sumber rujukan valid (jadwal resmi Kemenag/NASA, contoh manual kitab), atau
- Invariant algoritma yang harus selalu benar (mis. "ghurub selalu setelah
  ijtima'").

Contoh nyata: `AdDurrulAniqCalculatorTest.kt`, `EphemerisCalculatorTest.kt`.

**Kenapa tidak boleh sekadar mock data "asal"**: mock itu untuk mengisolasi
kode dari dunia luar (internet, waktu sistem) — bukan untuk membuktikan
kalkulasinya akurat. Membuktikan akurasi kalkulasi harus lewat perbandingan ke
rujukan yang sudah tervalidasi (golden value), bukan data karangan.

**Validasi silang antar-metode** (mis. `EphemerisCalculator` vs
`AdDurrulAniqCalculator` untuk input sama) juga masuk jalur ini — dua metode
beda tapi menghitung fenomena astronomis yang sama, jadi hasilnya harus
"berdekatan" dalam toleransi tertentu. Menentukan angka toleransi itu sendiri
butuh riset (lihat halaman Notion "Riset: Toleransi Selisih Ijtima'/Ghurub —
EphemerisCalculator vs Ad-Durrul Aniq" di database Ruang Angkasa, tag "Riset &
Publikasi", untuk contoh proses & sumbernya) — jangan pasang angka toleransi
tanpa dasar.

### Jalur 2 — Mock Test

Untuk kode yang punya dependency eksternal (ViewModel, Repo, apa pun yang
manggil API/Context/singleton lain): **pakai MockK**, supaya test tidak
beneran nembak internet/Android framework, dan supaya yang diuji murni
"apakah kode ini melakukan hal yang benar dengan dependency-nya", bukan
"apakah dependency-nya akurat" (itu urusan jalur 1).

Dua teknik mocking yang dipakai, tergantung bentuk dependency-nya:

1. **Dependency lewat constructor** (interface, mis. `AladhanApi` di-inject
   ke `PrayerRepository`): `mockk<AladhanApi>()` + `coEvery { ... } returns
   ...` (pakai `coEvery`, bukan `every`, karena fungsinya `suspend`).

2. **Dependency berupa Kotlin `object` (singleton)**, mis.
   `EphemerisCalculator`, `AdDurrulAniqCalculator`, `HilalPdfService` yang
   dipanggil langsung oleh `HilalViewModel` (bukan di-inject lewat
   constructor): **tidak bisa** dimock dengan `mockk<T>()` biasa. Pakai
   `mockkObject(NamaObject)` + `every { NamaObject.fn(...) } returns ...`
   ("static mocking"), dan **wajib** `unmockkAll()` di `@After` — object itu
   singleton JVM-wide, kalau tidak di-unmock, mock-nya bocor ke test lain
   yang jalan setelahnya di JVM/proses test yang sama.

Contoh nyata: `HilalViewModelTest.kt` (teknik 2), `PrayerTimesViewModel` /
`PrayerRepository` (teknik 1, contoh dibahas saat diskusi, belum ditulis
test-nya per 2026-09-06).

## Alat tambahan — pasang cuma kalau kodenya butuh

Jangan pasang semua alat di setiap test "just in case" — lihat dulu kodenya:

| Alat | Kapan dibutuhkan | Kenapa |
|---|---|---|
| `InstantTaskExecutorRule` (`androidx.arch.core:core-testing`) | Test membaca `LiveData.value` | LiveData didesain jalan di Main Thread Android asli; rule ini bikin dia jalan sinkron di JVM test |
| `kotlinx-coroutines-test` (`runTest` + `advanceUntilIdle()`) | Kode pakai `viewModelScope.launch` / `suspend fun` | Coroutine itu asynchronous — tanpa `advanceUntilIdle()`, assert bisa jalan sebelum coroutine-nya selesai (flaky) |
| Turbine | Kode pakai `Flow` | **Belum dipakai project ini** — semuanya masih 100% `LiveData`. Baru relevan kalau ada migrasi ke `Flow` |
| `mockkObject` + `unmockkAll` | Dependency berupa Kotlin `object` (bukan interface ter-inject) | Lihat "Jalur 2" di atas |

Contoh: `HilalViewModel.calculateHilal()` itu **sinkron**, bukan `suspend`,
tidak pakai `viewModelScope` — jadi `HilalViewModelTest` **tidak** butuh
`kotlinx-coroutines-test` sama sekali, meski dia ViewModel. Tapi
`PrayerTimesViewModel.fetchPrayerTimes()` pakai `viewModelScope.launch`, jadi
test-nya nanti wajib pakai `runTest`/`advanceUntilIdle()`. Cek kode dulu,
baru pilih alat — jangan template alat yang sama untuk semua ViewModel.

## Versi dependency test & catatan kompatibilitas

Repo ini masih pakai Kotlin Gradle plugin **1.7.10**. Beberapa versi terbaru
library test butuh Kotlin stdlib lebih baru dan akan gagal compile dengan
error `"Module was compiled with an incompatible version of Kotlin"`. Versi
yang sudah dikonfirmasi kompatibel (di `app/build.gradle`):

```gradle
testImplementation 'junit:junit:4.13.2'
testImplementation 'io.mockk:mockk:1.13.2'          // BUKAN versi terbaru (1.13.11+ butuh Kotlin stdlib 1.9, gagal compile)
testImplementation 'androidx.arch.core:core-testing:2.2.0'
```

Kalau mau upgrade Kotlin plugin project ke versi lebih baru di masa depan,
versi mockk di atas juga boleh di-upgrade — tapi cek dulu kompatibilitasnya,
jangan asal pakai versi paling baru.

## Alur menulis satu unit test

1. **Pilih target** — satu class/fungsi, tentukan jalurnya (golden/mock) dari
   pertanyaan di atas.
2. **Tulis skeleton** — nama-nama `@Test fun` dulu, isi `TODO()`. Ini
   checklist skenario, disepakati dulu sebelum ada yang menulis assertion —
   supaya tidak salah arah/lupa skenario penting (terutama untuk kode
   kompleks/kritis). Untuk kode sepele, boleh langsung tulis lengkap tanpa
   skeleton.
3. **Diskusi & sepakati cakupan** — skenario apa yang kurang? Edge case (data
   kosong, error API, lintang ekstrem, dst) sudah masuk?
4. **Isi Arrange-Act-Assert** — siapkan mock/data, panggil fungsi yang
   ditest, cek hasilnya.
5. **Jalankan & iterasi** — `./gradlew testDebugUnitTest --tests
   "<FQCN>"`. Gagal karena bug di kode → perbaiki kode. Gagal karena test
   salah asumsi → perbaiki test.
6. **Commit & update backlog** — centang baris checklist yang sesuai di
   backlog Notion.

## Referensi implementasi nyata di repo ini

- `app/src/test/java/site/elahady/alkaukaba/utils/addurrulaniq/AdDurrulAniqCalculatorTest.kt` — golden test, kewajaran fisis
- `app/src/test/java/site/elahady/alkaukaba/utils/EphemerisCalculatorTest.kt` — golden test + invariant algoritma + validasi silang antar-metode
- `app/src/test/java/site/elahady/alkaukaba/viewmodel/hilal/HilalViewModelTest.kt` — mock test, teknik `mockkObject` untuk dependency berupa singleton `object`
