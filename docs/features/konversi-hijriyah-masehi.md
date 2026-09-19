# Konversi Hijriyah - Masehi

### 1. Ringkasan (Overview)
- **Nama fitur**: Konversi Hijriyah - Masehi
- **Deskripsi singkat**: Konverter tanggal dua arah — Masehi → Hijriyah dan
  Hijriyah → Masehi — dalam satu layar dengan dua tab. Hasil (tanggal lengkap +
  nama hari) langsung diperbarui saat input berubah, tanpa tombol "Hitung".
  Memakai kalender Hijriyah **tabular** (aritmetika, offline), bukan hisab
  hakiki, jadi bisa selisih 1–2 hari dari hasil rukyat/hisab (lihat §4 dan §7).

### 2. Entry point & prasyarat
- Layar **Semua Menu** (`SemuaMenuActivity`), item **Konversi Hijriyah - Masehi**
  — posisinya tepat setelah **Kalkulator** dan sebelum **Al-Qur'an**.
- Tidak ada prasyarat permission/lokasi/internet/API key — murni hitungan lokal.

### 3. Titik masuk logika & navigasi
- [`SemuaMenuActivity.menuItems()`](../../app/src/main/java/Site/elahady/alkaukaba/ui/menu/SemuaMenuActivity.kt)
  — daftar menu grid; item baru diselipkan setelah `KalkulatorActivity`.
  Ikon `ic_menu_konversi_tanggal`, warna `bg_fuchsia_light`/`icon_fuchsia`.
- [`KonversiHijriyahActivity`](../../app/src/main/java/Site/elahady/alkaukaba/ui/konversitanggal/KonversiHijriyahActivity.kt)
  — satu-satunya layar fitur ini; tidak ada navigasi lanjutan.
- [`HijriDateUtil`](../../app/src/main/java/Site/elahady/alkaukaba/utils/HijriDateUtil.kt)
  — mesin konversi: `gregorianToHijri`, `hijriToGregorian`,
  `hijriMonthLength`, `isHijriLeapYear`, `gregorianMonthLength`, `weekdayName`,
  plus helper label (`hijriLabel`, `gregorianLabel`).

### 4. Struktur & alur data
- **Masehi → Hijriyah**: `DatePickerDialog` (rentang 1 Jan 1600 – 31 Des 2250)
  → `HijriDateUtil.gregorianToHijri` → label tanggal Hijriyah + "Hari X".
- **Hijriyah → Masehi**: input tanggal (angka), bulan (`Spinner`, 12 nama dari
  `HijriDateUtil.monthNames`), tahun (angka, 1000–1670 H) →
  validasi (`hijriMonthLength` menentukan 29/30 hari) →
  `HijriDateUtil.hijriToGregorian` → label Masehi lengkap dengan nama hari.
  Input tidak valid menampilkan pesan merah di kartu hasil, bukan crash.
- Default kedua tab = tanggal hari ini; link "Gunakan tanggal hari ini"
  mengembalikannya.
- **Algoritma**: Hijriyah tabular tipe sipil (Kuwaiti) — siklus 30 tahun dengan
  11 tahun kabisat (2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29), epoch 1 Muharram
  1 H = JDN 1.948.440. Bulan ganjil 30 hari, genap 29 hari, Dzulhijjah 30 hari
  di tahun kabisat. Arah Masehi → Hijriyah memakai `jdnToHijri` yang sudah ada
  (dipakai juga label bulan di `HijriCalendarEngine`); arah sebaliknya
  (`hijriToJdn`) rumus invers-nya, hasil JDN dikonversi ke Gregorian.
- **Kenapa rentang dibatasi**: sebelum reformasi Gregorian (Okt 1582) tanggal
  sejarah tercatat dalam kalender Julian — hasil Gregorian proleptik di rentang
  itu akan menyesatkan. Batas atas hanya untuk menghindari input tidak masuk akal.
- **Berbeda dari kalender di beranda**: grid kalender beranda & fitur Awal Bulan
  memakai hisab hakiki (ijtima' + kriteria Neo-MABIMS, tergantung lokasi).
  Konverter ini tabular, jadi hasilnya bisa selisih 1–2 hari dari beranda. Catatan
  ini juga tampil di layar. Pergantian tanggal dihitung tengah malam, bukan maghrib.

### 5. Dependencies & tech stack khusus
Tidak ada — `DatePickerDialog`, `Spinner`, `TabLayout` bawaan/Material yang sudah
dipakai fitur lain (pola tab sama dengan `ZakatActivity`). Tanpa library tanggal
tambahan (`java.time` sengaja tidak dipakai di kode produksi karena minSdk 21).

### 6. Testing
[`HijriDateUtilTest`](../../app/src/test/java/site/elahady/alkaukaba/utils/HijriDateUtilTest.kt)
— golden/invariant test tanpa mock (Jalur 1 di `strategi-unit-test.md`):
epoch 1 H = Jumat; nama hari rujukan (17 Agu 1945 Jumat, 1 Jan 2000 Sabtu);
siklus 30 tahun = 10.631 hari; round trip Masehi→Hijriyah→Masehi untuk **setiap
hari** 1600–2250; `hijriToGregorian` maju tepat 1 hari untuk tiap hari Hijriyah
berurutan (1000–1700 H, cek silang ke `java.time.LocalDate`); pergantian bulan;
selisih ≤ 2 hari terhadap Umm al-Qura (`HijrahDate`, 1400–1500 H); konsistensi
dengan `fullDateLabel` lama.

Jalankan: `./gradlew.bat testDebugUnitTest --tests "site.elahady.alkaukaba.utils.HijriDateUtilTest"`.

Verifikasi manual (emulator, 2026-09-19): kedua tab tampil benar; 19 September
2026 ↔ 6 Rabiul Akhir 1448 H (Sabtu) konsisten dua arah. Pesan validasi (tanggal
30 di bulan 29 hari, tahun di luar rentang) dan `DatePickerDialog` belum
diverifikasi visual — hanya lewat pembacaan kode.

### 7. Known issues & TODOs
- [ ] Hasil tabular bisa selisih 1–2 hari dari hisab hakiki/beranda (by design,
      dicatat di layar). Opsi lanjutan: toggle "Hisab (Neo-MABIMS)" untuk arah
      Masehi → Hijriyah lewat `HijriCalendarEngine.fullDateLabelFor` (butuh lokasi,
      lebih berat, dan arah sebaliknya perlu iterasi bulan).
- [ ] Belum menampilkan pasaran Jawa (sudah ada `JavaneseCalendarUtil`) maupun
      tombol salin/bagikan hasil.
- [ ] Pesan validasi & `DatePickerDialog` belum dicek visual di emulator.
